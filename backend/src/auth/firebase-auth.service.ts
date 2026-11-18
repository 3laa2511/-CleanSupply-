import { Injectable, UnauthorizedException } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { applicationDefault, getApps, initializeApp } from 'firebase-admin/app';
import { DecodedIdToken, getAuth } from 'firebase-admin/auth';

@Injectable()
export class FirebaseAuthService {
  constructor(private readonly config: ConfigService) {}

  async verifyIdToken(idToken: string): Promise<DecodedIdToken> {
    try {
      const projectId = this.config.get<string>('FIREBASE_PROJECT_ID') ??
        this.config.get<string>('GOOGLE_CLOUD_PROJECT');
      const app = getApps()[0] ?? initializeApp({
        credential: applicationDefault(),
        ...(projectId ? { projectId } : {}),
      });
      return await getAuth(app).verifyIdToken(idToken, true);
    } catch {
      throw new UnauthorizedException('Invalid or expired authentication token');
    }
  }
}
