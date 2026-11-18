import { SetMetadata } from '@nestjs/common';
import { IS_PUBLIC_KEY, REQUIRED_PERMISSIONS_KEY } from './auth.constants';
export const Public = () => SetMetadata(IS_PUBLIC_KEY, true);
export const RequirePermissions = (...permissions: string[]) => SetMetadata(REQUIRED_PERMISSIONS_KEY, permissions);
