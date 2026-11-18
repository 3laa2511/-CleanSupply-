import {
  ArgumentsHost,
  Catch,
  ExceptionFilter,
  HttpException,
  HttpStatus,
} from '@nestjs/common';
import { Logger } from 'nestjs-pino';

@Catch()
export class HttpExceptionFilter implements ExceptionFilter {
  constructor(private readonly logger: Logger) {}

  catch(exception: unknown, host: ArgumentsHost): void {
    const http = host.switchToHttp();
    const request = http.getRequest<{
      method?: string;
      url?: string;
      id?: string;
    }>();
    const response = http.getResponse<{
      status(code: number): this;
      json(body: unknown): void;
    }>();

    const status =
      exception instanceof HttpException
        ? exception.getStatus()
        : HttpStatus.INTERNAL_SERVER_ERROR;
    const message =
      exception instanceof HttpException
        ? exception.getResponse()
        : 'Internal server error';

    if (status >= 500) {
      this.logger.error(
        {
          err: exception,
          requestId: request.id,
          method: request.method,
          url: request.url,
        },
        'Unhandled request exception',
      );
    }

    response.status(status).json({
      statusCode: status,
      message,
      requestId: request.id ?? null,
      timestamp: new Date().toISOString(),
    });
  }
}
