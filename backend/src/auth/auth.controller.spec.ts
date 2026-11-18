import { ConflictException } from '@nestjs/common';
import { Prisma } from '@prisma/client';
import { AuthController } from './auth.controller';
import { PrismaService } from '../prisma/prisma.service';

describe('AuthController', () => {
  let controller: AuthController;
  let prisma: { user: { upsert: jest.Mock } };

  beforeEach(() => {
    prisma = { user: { upsert: jest.fn() } };
    controller = new AuthController(prisma as unknown as PrismaService);
  });

  it('creates a minimal internal profile from a verified Firebase identity', async () => {
    prisma.user.upsert.mockResolvedValue({
      id: 'user-1', email: null, phone: '+201000000000', displayName: null,
      status: 'ACTIVE', roles: [],
    });

    const result = await controller.me({
      auth: { uid: 'firebase-uid', phone_number: '+201000000000' },
    } as never);

    expect(prisma.user.upsert).toHaveBeenCalledWith(expect.objectContaining({
      where: { firebaseUid: 'firebase-uid' },
      create: {
        firebaseUid: 'firebase-uid', email: null,
        phone: '+201000000000', displayName: null,
      },
      update: { phone: '+201000000000' },
    }));
    expect(result).toMatchObject({ id: 'user-1', status: 'ACTIVE', roles: [] });
  });

  it('does not clear optional profile fields when Firebase omits claims', async () => {
    prisma.user.upsert.mockResolvedValue({
      id: 'user-1', email: 'known@example.com', phone: '+201000000000',
      displayName: 'Known Name', status: 'ACTIVE', roles: [],
    });

    await controller.me({ auth: { uid: 'firebase-uid' } } as never);

    expect(prisma.user.upsert).toHaveBeenCalledWith(expect.objectContaining({
      update: {},
    }));
  });

  it('maps unique email or phone conflicts to a safe conflict response', async () => {
    prisma.user.upsert.mockRejectedValue(
      new Prisma.PrismaClientKnownRequestError('Unique constraint failed', {
        code: 'P2002', clientVersion: 'test',
      }),
    );

    await expect(controller.me({ auth: { uid: 'firebase-uid', email: 'taken@example.com' } } as never))
      .rejects.toBeInstanceOf(ConflictException);
  });

  it('rejects requests without a verified Firebase identity', async () => {
    await expect(controller.me({} as never)).rejects.toBeInstanceOf(ConflictException);
    expect(prisma.user.upsert).not.toHaveBeenCalled();
  });
});
