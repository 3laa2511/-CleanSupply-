import { CanActivate, ExecutionContext, ForbiddenException, Injectable } from '@nestjs/common';
import { Reflector } from '@nestjs/core';
import { PrismaService } from '../prisma/prisma.service';
import { REQUIRED_PERMISSIONS_KEY } from './auth.constants';
import { AuthenticatedRequest } from './authenticated-request';

@Injectable()
export class PermissionsGuard implements CanActivate {
  constructor(private readonly reflector: Reflector, private readonly prisma: PrismaService) {}

  async canActivate(context: ExecutionContext): Promise<boolean> {
    const required = this.reflector.getAllAndOverride<string[]>(REQUIRED_PERMISSIONS_KEY, [
      context.getHandler(), context.getClass(),
    ]);
    if (!required?.length) return true;

    const firebaseUid = context.switchToHttp().getRequest<AuthenticatedRequest>().auth?.uid;
    if (!firebaseUid) throw new ForbiddenException('Authenticated identity is missing');

    const user = await this.prisma.user.findUnique({
      where: { firebaseUid },
      select: {
        status: true,
        roles: { select: { role: { select: {
          permissions: { select: { permission: { select: { code: true } } } },
        } } } },
      },
    });
    if (!user || user.status !== 'ACTIVE') throw new ForbiddenException('User is not active in CleanSupply');

    const granted = new Set(user.roles.flatMap(({ role }) =>
      role.permissions.map(({ permission }) => permission.code),
    ));
    if (!required.every((permission) => granted.has(permission))) {
      throw new ForbiddenException('You do not have permission to perform this action');
    }
    return true;
  }
}
