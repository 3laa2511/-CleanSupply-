import { ConflictException, Controller, Get, Req } from '@nestjs/common';
import { ApiBearerAuth, ApiTags } from '@nestjs/swagger';
import { Prisma } from '@prisma/client';
import { AuthenticatedRequest } from './authenticated-request';
import { PrismaService } from '../prisma/prisma.service';

@ApiTags('Authentication')
@ApiBearerAuth()
@Controller('auth')
export class AuthController {
  constructor(private readonly prisma: PrismaService) {}

  @Get('me')
  async me(@Req() request: AuthenticatedRequest) {
    const claims = request.auth;
    if (!claims?.uid) throw new ConflictException('Verified Firebase identity is unavailable');

    // Firebase may omit optional claims (for example, a phone-only account has no email).
    // Do not erase profile data that was previously collected through another trusted flow.
    const profileFields = {
      ...(claims.email ? { email: claims.email } : {}),
      ...(claims.phone_number ? { phone: claims.phone_number } : {}),
      ...(claims.name ? { displayName: claims.name } : {}),
    };

    try {
      const user = await this.prisma.user.upsert({
        where: { firebaseUid: claims.uid },
        update: profileFields,
        create: {
          firebaseUid: claims.uid,
          email: claims.email ?? null,
          phone: claims.phone_number ?? null,
          displayName: claims.name ?? null,
        },
        select: {
          id: true, email: true, phone: true, displayName: true, status: true,
          roles: { select: { role: { select: {
            code: true, name: true,
            permissions: { select: { permission: { select: { code: true } } } },
          } } } },
        },
      });
      return {
        id: user.id, email: user.email, phone: user.phone,
        displayName: user.displayName, status: user.status,
        roles: user.roles.map(({ role }) => ({
          code: role.code, name: role.name,
          permissions: role.permissions.map(({ permission }) => permission.code),
        })),
      };
    } catch (error) {
      if (error instanceof Prisma.PrismaClientKnownRequestError && error.code === 'P2002') {
        throw new ConflictException('This email or phone is linked to another account');
      }
      throw error;
    }
  }
}
