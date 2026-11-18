import { ServiceUnavailableException } from '@nestjs/common';
import { HealthController } from './health.controller';
import { PrismaService } from '../prisma/prisma.service';

describe('HealthController', () => {
  it('reports healthy when PostgreSQL responds', async () => {
    const prisma = { isReady: jest.fn().mockResolvedValue(true) } as unknown as PrismaService;
    const controller = new HealthController(prisma);

    const result = await controller.check();

    expect(result.status).toBe('ok');
    expect(result.dependencies.postgres).toBe('ok');
    expect(prisma.isReady).toHaveBeenCalledTimes(1);
  });

  it('returns service unavailable when PostgreSQL is down', async () => {
    const prisma = { isReady: jest.fn().mockResolvedValue(false) } as unknown as PrismaService;
    const controller = new HealthController(prisma);

    await expect(controller.check()).rejects.toBeInstanceOf(ServiceUnavailableException);
  });
});
