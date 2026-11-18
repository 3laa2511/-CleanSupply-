import { MiddlewareConsumer, Module, NestModule } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import { APP_GUARD } from '@nestjs/core';
import { ThrottlerGuard, ThrottlerModule } from '@nestjs/throttler';
import { LoggerModule } from 'nestjs-pino';
import { AuthModule } from './auth/auth.module';
import { FirebaseAuthGuard } from './auth/firebase-auth.guard';
import { PermissionsGuard } from './auth/permissions.guard';
import { CatalogModule } from './catalog/catalog.module';
import { CorrelationIdMiddleware } from './common/correlation-id.middleware';
import { envValidationSchema } from './config/env.validation';
import { HealthModule } from './health/health.module';
import { PrismaModule } from './prisma/prisma.module';

@Module({
  imports: [
    ConfigModule.forRoot({ isGlobal: true, envFilePath: ['.env', '../.env'], validationSchema: envValidationSchema, validationOptions: { abortEarly: false, allowUnknown: true } }),
    LoggerModule.forRoot({ pinoHttp: {
      level: process.env.LOG_LEVEL ?? 'info',
      redact: { paths: ['req.headers.authorization','req.headers.cookie','req.body.password','req.body.idToken','req.body.paymentReference'], censor: '[REDACTED]' },
      serializers: { req: (req) => ({ id: req.id, method: req.method, url: req.url }) },
    } }),
    ThrottlerModule.forRoot([{ ttl: 60_000, limit: 120 }]),
    PrismaModule, AuthModule, CatalogModule, HealthModule,
  ],
  providers: [
    { provide: APP_GUARD, useClass: ThrottlerGuard },
    { provide: APP_GUARD, useClass: FirebaseAuthGuard },
    { provide: APP_GUARD, useClass: PermissionsGuard },
  ],
})
export class AppModule implements NestModule {
  configure(consumer: MiddlewareConsumer): void {
    consumer.apply(CorrelationIdMiddleware).forRoutes('*');
  }
}
