import { randomUUID } from 'node:crypto';
import { Injectable, NestMiddleware } from '@nestjs/common';
import type { NextFunction, Request, Response } from 'express';

@Injectable()
export class CorrelationIdMiddleware implements NestMiddleware {
  use(
    request: Request & { id?: string },
    response: Response,
    next: NextFunction,
  ): void {
    const incoming = request.header('x-correlation-id');
    const id =
      incoming && /^[a-zA-Z0-9._-]{1,100}$/.test(incoming)
        ? incoming
        : randomUUID();
    request.id = id;
    response.setHeader('x-correlation-id', id);
    next();
  }
}
