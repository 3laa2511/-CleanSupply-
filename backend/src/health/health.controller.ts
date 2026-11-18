import { Controller, Get, ServiceUnavailableException } from '@nestjs/common';
import { Public } from '../auth/auth.decorators';
import { PrismaService } from '../prisma/prisma.service';

@Controller('health')
export class HealthController {
  constructor(private readonly prisma: PrismaService) {}

  @Public()
  @Get()
  async check(): Promise<{ status: 'ok'; service: string; timestamp: string; dependencies: { postgres: 'ok' } }> {
    const databaseReady = await this.prisma.isReady();
    if (!databaseReady) {
      throw new ServiceUnavailableException({ status: 'error', dependencies: { postgres: 'unavailable' } });
    }
    return { status: 'ok', service: 'cleansupply-api', timestamp: new Date().toISOString(), dependencies: { postgres: 'ok' } };
  }
}
