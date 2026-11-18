import { Controller, Get, NotFoundException, Param, Query } from '@nestjs/common';
import { ApiOperation, ApiTags } from '@nestjs/swagger';
import { Public } from '../auth/auth.decorators';
import { CatalogQueryDto } from './dto/catalog-query.dto';
import { CatalogService } from './catalog.service';

@ApiTags('Catalog')
@Public()
@Controller('catalog')
export class CatalogController {
  constructor(private readonly catalog: CatalogService) {}

  @Get('categories')
  @ApiOperation({ summary: 'List active catalog categories' })
  categories() {
    return this.catalog.categories();
  }

  @Get('products')
  @ApiOperation({ summary: 'Search and browse active products with retail pricing and available stock' })
  products(@Query() query: CatalogQueryDto) {
    return this.catalog.products(query);
  }

  @Get('products/:slug')
  @ApiOperation({ summary: 'Get an active product by slug' })
  async product(@Param('slug') slug: string) {
    const product = await this.catalog.productBySlug(slug);
    if (!product) throw new NotFoundException('Product not found');
    return { data: product };
  }
}
