import { Injectable } from '@nestjs/common';
import { Prisma, CustomerSegment, CustomerTier } from '@prisma/client';
import { PrismaService } from '../prisma/prisma.service';
import { CatalogQueryDto } from './dto/catalog-query.dto';

@Injectable()
export class CatalogService {
  constructor(private readonly prisma: PrismaService) {}

  async categories() {
    const rows = await this.prisma.category.findMany({
      where: { isActive: true },
      orderBy: [{ sortOrder: 'asc' }, { nameEn: 'asc' }],
      select: { id: true, slug: true, nameAr: true, nameEn: true, iconKey: true, sortOrder: true },
    });
    return { data: rows };
  }

  async products(query: CatalogQueryDto) {
    const page = query.page ?? 1;
    const limit = query.limit ?? 20;
    const now = new Date();
    const where: Prisma.ProductWhereInput = {
      isActive: true,
      ...(query.featured === undefined ? {} : { featured: query.featured }),
      ...(query.category ? { category: { is: { slug: query.category, isActive: true } } } : {}),
      ...(query.q?.trim() ? {
        OR: [
          { nameAr: { contains: query.q.trim(), mode: 'insensitive' } },
          { nameEn: { contains: query.q.trim(), mode: 'insensitive' } },
          { sku: { contains: query.q.trim(), mode: 'insensitive' } },
          { brand: { contains: query.q.trim(), mode: 'insensitive' } },
        ],
      } : {}),
    };

    const [total, rows] = await this.prisma.$transaction([
      this.prisma.product.count({ where }),
      this.prisma.product.findMany({
        where,
        orderBy: [{ featured: 'desc' }, { nameEn: 'asc' }],
        skip: (page - 1) * limit,
        take: limit,
        select: {
          id: true, sku: true, slug: true, nameAr: true, nameEn: true,
          descriptionAr: true, descriptionEn: true, brand: true,
          unitLabelAr: true, unitLabelEn: true, baseUnit: true,
          cartonSize: true, unitsPerCarton: true, minOrderQty: true,
          bulkMinQty: true, bulkDiscountPercent: true,
          moqRetail: true, moqWholesale: true, moqBusiness: true,
          imageKey: true, featured: true,
          category: { select: { id: true, slug: true, nameAr: true, nameEn: true, iconKey: true } },
          prices: {
            where: {
              segment: CustomerSegment.RETAIL, tier: CustomerTier.STANDARD,
              validFrom: { lte: now },
              OR: [{ validUntil: null }, { validUntil: { gt: now } }],
            },
            orderBy: { validFrom: 'desc' }, take: 1,
            select: { unitPrice: true, currency: true },
          },
          balances: { select: { quantity: true, reservedQuantity: true } },
        },
      }),
    ]);

    return {
      data: rows.map((product) => {
        const available = product.balances.reduce((sum, balance) => sum + balance.quantity - balance.reservedQuantity, 0);
        const price = product.prices[0];
        return {
          id: product.id, sku: product.sku, slug: product.slug,
          nameAr: product.nameAr, nameEn: product.nameEn,
          descriptionAr: product.descriptionAr, descriptionEn: product.descriptionEn,
          brand: product.brand, unitLabelAr: product.unitLabelAr, unitLabelEn: product.unitLabelEn,
          baseUnit: product.baseUnit, cartonSize: product.cartonSize,
          unitsPerCarton: product.unitsPerCarton, minOrderQty: product.minOrderQty,
          bulkMinQty: product.bulkMinQty, bulkDiscountPercent: product.bulkDiscountPercent,
          moqRetail: product.moqRetail, moqWholesale: product.moqWholesale,
          moqBusiness: product.moqBusiness, imageKey: product.imageKey,
          featured: product.featured, category: product.category,
          retailPrice: price ? Number(price.unitPrice) : null,
          currency: price?.currency ?? 'EGP', stockAvailable: Math.max(0, available),
        };
      }),
      meta: { page, limit, total, totalPages: Math.ceil(total / limit) },
    };
  }

  async productBySlug(slug: string) {
    const now = new Date();
    const product = await this.prisma.product.findFirst({
      where: { slug, isActive: true },
      select: {
        id: true, sku: true, slug: true, nameAr: true, nameEn: true,
        descriptionAr: true, descriptionEn: true, brand: true,
        unitLabelAr: true, unitLabelEn: true, baseUnit: true,
        cartonSize: true, unitsPerCarton: true, minOrderQty: true,
        bulkMinQty: true, bulkDiscountPercent: true,
        moqRetail: true, moqWholesale: true, moqBusiness: true,
        imageKey: true, featured: true,
        category: { select: { id: true, slug: true, nameAr: true, nameEn: true, iconKey: true } },
        prices: {
          where: {
            segment: CustomerSegment.RETAIL, tier: CustomerTier.STANDARD,
            validFrom: { lte: now },
            OR: [{ validUntil: null }, { validUntil: { gt: now } }],
          },
          orderBy: { validFrom: 'desc' }, take: 1,
          select: { unitPrice: true, currency: true },
        },
        balances: { select: { quantity: true, reservedQuantity: true } },
      },
    });
    if (!product) return null;
    const available = product.balances.reduce((sum, balance) => sum + balance.quantity - balance.reservedQuantity, 0);
    const price = product.prices[0];
    return {
      id: product.id, sku: product.sku, slug: product.slug,
      nameAr: product.nameAr, nameEn: product.nameEn,
      descriptionAr: product.descriptionAr, descriptionEn: product.descriptionEn,
      brand: product.brand, unitLabelAr: product.unitLabelAr, unitLabelEn: product.unitLabelEn,
      baseUnit: product.baseUnit, cartonSize: product.cartonSize,
      unitsPerCarton: product.unitsPerCarton, minOrderQty: product.minOrderQty,
      bulkMinQty: product.bulkMinQty, bulkDiscountPercent: product.bulkDiscountPercent,
      moqRetail: product.moqRetail, moqWholesale: product.moqWholesale,
      moqBusiness: product.moqBusiness, imageKey: product.imageKey,
      featured: product.featured, category: product.category,
      retailPrice: price ? Number(price.unitPrice) : null,
      currency: price?.currency ?? 'EGP', stockAvailable: Math.max(0, available),
    };
  }
}
