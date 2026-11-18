import { Module } from '@nestjs/common';
import { AuthController } from './auth.controller';
import { FirebaseAuthService } from './firebase-auth.service';
import { FirebaseAuthGuard } from './firebase-auth.guard';
import { PermissionsGuard } from './permissions.guard';
@Module({ controllers: [AuthController], providers: [FirebaseAuthService, FirebaseAuthGuard, PermissionsGuard], exports: [FirebaseAuthService, FirebaseAuthGuard, PermissionsGuard] })
export class AuthModule {}
