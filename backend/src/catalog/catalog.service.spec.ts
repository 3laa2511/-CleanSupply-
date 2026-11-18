import { CatalogService } from './catalog.service';
import { PrismaService } from '../prisma/prisma.service';

describe('CatalogService', () => {
  const prisma = {
    category: { findMany: jest.fn() },
    product: { count: jest.fn(), findMany: jest.fn(), findFirst: jest.fn() },
    $transaction: jest.fn(),
  };
  let service: CatalogService;

  beforeEach(() => {
    jest.clearAllMocks();
    service = new CatalogService(prisma as unknown as PrismaService);
  });

  it('returns active categories in display order', async () => {
    prisma.category.findMany.mockResolvedValue([{ id: 'c1', slug: 'cleaning' }]);
    await expect(service.categories()).resolves.toEqual({ data: [{ id: 'c1', slug: 'cleaning' }] });
    expect(prisma.category.findMany).toHaveBeenCalledWith(expect.objectContaining({
      where: { isActive: true }, orderBy: [{ sortOrder: 'asc' }, { nameEn: 'asc' }],
    }));
  });

  it('paginates products and maps retail price and available stock', async () => {
    prisma.$transaction.mockResolvedValue([
      1,
      [{
        id: 'p1', sku: 'SKU-1', slug: 'soap', nameAr: 'صابون', nameEn: 'Soap',
        descriptionAr: null, descriptionEn: null, brand: 'Brand',
        unitLabelAr: 'قطعة', unitLabelEn: 'piece', baseUnit: 'piece',
        cartonSize: 12, unitsPerCarton: 12, minOrderQty: 1, bulkMinQty: 10,
        bulkDiscountPercent: 0, moqRetail: 1, moqWholesale: 5, moqBusiness: 10,
        imageKey: 'detergents', featured: true,
        category: { id: 'c1', slug: 'cleaning', nameAr: 'تنظيف', nameEn: 'Cleaning', iconKey: 'detergents' },
        prices: [{ unitPrice: { toString: () => '35.50' }, currency: 'EGP' }],
        balances: [{ quantity: 20, reservedQuantity: 4 }],
      }],
    ]);

    const result = await service.products({ page: 1, limit: 20 });
    expect(result.meta).toEqual({ page: 1, limit: 20, total: 1, totalPages: 1 });
    expect(result.data[0]).toMatchObject({ retailPrice: 35.5, stockAvailable: 16, slug: 'soap' });
  });

  it('returns null for an unknown product slug', async () => {
    prisma.product.findFirst.mockResolvedValue(null);
    await expect(service.productBySlug('missing')).resolves.toBeNull();
  });
});
