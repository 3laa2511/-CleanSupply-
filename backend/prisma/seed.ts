import { PrismaClient, CustomerSegment, CustomerTier } from '@prisma/client';

const prisma = new PrismaClient();

const roleDefinitions = [
  { code: 'SUPER_ADMIN', name: 'Super Admin', description: 'Full system administration' },
  { code: 'OPERATIONS', name: 'Operations', description: 'Order and operational workflows' },
  { code: 'INVENTORY', name: 'Inventory', description: 'Inventory and stock operations' },
  { code: 'PURCHASING', name: 'Purchasing', description: 'Suppliers, purchasing and goods receipts' },
  { code: 'FINANCE', name: 'Finance', description: 'Payments, credit and financial reconciliation' },
  { code: 'DELIVERY', name: 'Delivery', description: 'Dispatch and delivery operations' },
  { code: 'SUPPORT', name: 'Customer Support', description: 'Customer support and tickets' },
] as const;

const permissionDefinitions = [
  'dashboard.read', 'users.read', 'users.manage',
  'catalog.read', 'catalog.manage', 'pricing.read', 'pricing.manage',
  'inventory.read', 'inventory.manage', 'purchasing.read', 'purchasing.manage',
  'orders.read', 'orders.manage', 'payments.read', 'payments.reconcile',
  'credit.manage', 'delivery.read', 'delivery.manage', 'support.read',
  'support.manage', 'audit.read', 'settings.manage',
] as const;

const rolePermissions: Record<string, readonly string[]> = {
  SUPER_ADMIN: permissionDefinitions,
  OPERATIONS: ['dashboard.read', 'orders.read', 'orders.manage', 'delivery.read', 'delivery.manage', 'support.read'],
  INVENTORY: ['dashboard.read', 'catalog.read', 'inventory.read', 'inventory.manage'],
  PURCHASING: ['dashboard.read', 'purchasing.read', 'purchasing.manage', 'inventory.read'],
  FINANCE: ['dashboard.read', 'payments.read', 'payments.reconcile', 'credit.manage', 'orders.read', 'audit.read'],
  DELIVERY: ['dashboard.read', 'orders.read', 'delivery.read', 'delivery.manage'],
  SUPPORT: ['dashboard.read', 'users.read', 'orders.read', 'support.read', 'support.manage'],
};

const categories = [
  { slug: 'detergents-laundry', nameAr: 'المنظفات والغسيل', nameEn: 'Detergents & Laundry', iconKey: 'detergents', sortOrder: 1 },
  { slug: 'disinfectants', nameAr: 'المطهرات والمعقمات', nameEn: 'Disinfectants', iconKey: 'disinfectants', sortOrder: 2 },
  { slug: 'paper-tissues', nameAr: 'الورقيات والمناديل', nameEn: 'Paper & Tissues', iconKey: 'paper', sortOrder: 3 },
  { slug: 'cleaning-tools', nameAr: 'أدوات ومعدات النظافة', nameEn: 'Cleaning Tools', iconKey: 'tools', sortOrder: 4 },
  { slug: 'industrial-chemicals', nameAr: 'الكيماويات الصناعية', nameEn: 'Industrial Chemicals', iconKey: 'industrial', sortOrder: 5 },
] as const;

const products = [
  { sku: 'CS-GEL-5L', slug: 'concentrated-laundry-gel-5l', category: 'detergents-laundry', nameAr: 'جل غسيل أوتوماتيك مركز 5 لتر', nameEn: 'Concentrated Automatic Laundry Gel 5L', descriptionAr: 'تركيبة إنزيمية عالية التركيز للملابس والمغاسل التجارية برائحة النسيم المنعش.', descriptionEn: 'High-efficiency enzymatic detergent jerrycan built for homes and commercial laundries.', brand: 'PureWave', unitLabelAr: 'جركن 5 لتر', unitLabelEn: '5L Jerrycan', baseUnit: 'JERRYCAN', cartonSize: 4, bulkMinQty: 8, bulkDiscountPercent: 10, moqRetail: 1, moqWholesale: 4, moqBusiness: 8, imageKey: 'detergents', featured: true, stock: 160, retail: 215, wholesale: 178, business: 162 },
  { sku: 'CS-DSH-4L', slug: 'lemon-dish-liquid-4l', category: 'detergents-laundry', nameAr: 'سائل جلي ليمون مركز 4 لتر', nameEn: 'Ultra Degreasing Dish Liquid 4L', descriptionAr: 'يزيل الدهون الصعبة بكفاءة عالية للمطاعم والمطابخ الكبيرة، لطيف على اليدين.', descriptionEn: 'Heavy-duty lemon degreaser formulated for restaurant kitchens and household use.', brand: 'SparkPro', unitLabelAr: 'جركن 4 لتر', unitLabelEn: '4L Bottle', baseUnit: 'JERRYCAN', cartonSize: 4, bulkMinQty: 12, bulkDiscountPercent: 8, moqRetail: 1, moqWholesale: 4, moqBusiness: 8, imageKey: 'detergents', featured: true, stock: 220, retail: 135, wholesale: 108, business: 96 },
  { sku: 'CS-SAN-1L', slug: 'pine-chlorine-disinfectant-1l', category: 'disinfectants', nameAr: 'مطهر أرضيات بالصنوبر والكلورين 1 لتر', nameEn: 'Pine & Chlorine Surface Disinfectant 1L', descriptionAr: 'مطهر للأسطح والأرضيات برائحة الصنوبر.', descriptionEn: 'Pine-scented surface and floor disinfectant.', brand: 'SterilGuard', unitLabelAr: 'عبوة 1 لتر', unitLabelEn: '1L Bottle', baseUnit: 'BOTTLE', cartonSize: 12, bulkMinQty: 24, bulkDiscountPercent: 12, moqRetail: 1, moqWholesale: 12, moqBusiness: 24, imageKey: 'detergents', featured: true, stock: 420, retail: 58, wholesale: 44, business: 39 },
  { sku: 'CS-GLS-750', slug: 'glass-steel-polish-750ml', category: 'disinfectants', nameAr: 'ملمع زجاج وأسطح ستانلس 750 مل', nameEn: 'Streak-Free Glass & Steel Polish 750ml', descriptionAr: 'يجف سريعاً بدون خطوط للواجهات الزجاجية والمكاتب والفنادق.', descriptionEn: 'Quick-dry spray for spotless glass facades and stainless trim.', brand: 'ClearSight', unitLabelAr: 'بخاخ 750 مل', unitLabelEn: '750ml Trigger', baseUnit: 'BOTTLE', cartonSize: 12, bulkMinQty: 12, bulkDiscountPercent: 10, moqRetail: 1, moqWholesale: 6, moqBusiness: 12, imageKey: 'detergents', featured: false, stock: 190, retail: 65, wholesale: 51, business: 45 },
  { sku: 'CS-PPR-JMB', slug: 'commercial-jumbo-paper-roll', category: 'paper-tissues', nameAr: 'رول مناشف ورقية جامبو طبقتين', nameEn: '2-Ply Commercial Jumbo Paper Roll', descriptionAr: 'امتصاص فائق للمنشآت الطبية والمطاعم، 300 متر للرول.', descriptionEn: 'High-absorbency 300m jumbo towel roll for restrooms and hospitality.', brand: 'SoftPly', unitLabelAr: 'لفة جامبو', unitLabelEn: 'Jumbo Roll', baseUnit: 'ROLL', cartonSize: 6, bulkMinQty: 12, bulkDiscountPercent: 12, moqRetail: 1, moqWholesale: 6, moqBusiness: 12, imageKey: 'paper', featured: true, stock: 310, retail: 110, wholesale: 86, business: 78 },
  { sku: 'CS-TIS-550', slug: 'facial-tissue-550-sheets', category: 'paper-tissues', nameAr: 'علب مناديل سحب 550 منديل (3 طبقات)', nameEn: 'Facial Tissue Box 550 Sheets (3-Ply)', descriptionAr: 'نعومة فائقة وتغليف أنيق يناسب الفنادق والمكاتب والمنازل.', descriptionEn: 'Ultra-soft 3-ply facial tissue boxes for retail and corporate offices.', brand: 'SoftPly', unitLabelAr: 'عبوة 3 علب', unitLabelEn: '3-Box Bundle', baseUnit: 'PACK', cartonSize: 10, bulkMinQty: 20, bulkDiscountPercent: 10, moqRetail: 1, moqWholesale: 10, moqBusiness: 20, imageKey: 'paper', featured: true, stock: 500, retail: 95, wholesale: 76, business: 69 },
  { sku: 'CS-MOP-PRO', slug: 'professional-microfiber-flat-mop', category: 'cleaning-tools', nameAr: 'ممسحة مايكروفايبر ألمنيوم احترافية', nameEn: 'Anodized Microfiber Flat Mop Pro', descriptionAr: 'يد تلسكوبية متينة مع 2 غيار مايكروفايبر قابل للغسل للأرضيات الرخامية.', descriptionEn: 'Telescopic aluminum frame with two washable microfiber pads.', brand: 'ApexTools', unitLabelAr: 'طقم كامل', unitLabelEn: 'Complete Kit', baseUnit: 'KIT', cartonSize: 6, bulkMinQty: 6, bulkDiscountPercent: 8, moqRetail: 1, moqWholesale: 3, moqBusiness: 6, imageKey: 'tools', featured: true, stock: 75, retail: 340, wholesale: 275, business: 250 },
  { sku: 'CS-BKT-24L', slug: 'commercial-wringer-bucket-24l', category: 'cleaning-tools', nameAr: 'جردل تنظيف بعصارة 24 لتر وعجلات', nameEn: '24L Commercial Wringer Mop Bucket', descriptionAr: 'بلاستيك مقوى مقاوم للصدمات مع علامات تحذير واضحة.', descriptionEn: 'Impact-resistant 24L wheeled wringer bucket for commercial janitorial teams.', brand: 'ApexTools', unitLabelAr: 'وحدة 24 لتر', unitLabelEn: '24L Unit', baseUnit: 'UNIT', cartonSize: 2, bulkMinQty: 4, bulkDiscountPercent: 10, moqRetail: 1, moqWholesale: 2, moqBusiness: 4, imageKey: 'tools', featured: false, stock: 44, retail: 690, wholesale: 560, business: 510 },
  { sku: 'CS-IND-20L', slug: 'industrial-heavy-degreaser-20l', category: 'industrial-chemicals', nameAr: 'مزيل شحوم صناعي مركز 20 لتر', nameEn: 'Industrial Heavy Degreaser Drum 20L', descriptionAr: 'مخصص للمصانع وورش الصيانة ومطابخ الفنادق لإزالة الزيوت والشحوم المتراكمة.', descriptionEn: 'Heavy-duty workshop and factory floor degreaser in a sealed 20L drum.', brand: 'ChemForce', unitLabelAr: 'بستلة 20 لتر', unitLabelEn: '20L Drum', baseUnit: 'DRUM', cartonSize: 1, bulkMinQty: 5, bulkDiscountPercent: 12, moqRetail: 1, moqWholesale: 2, moqBusiness: 3, imageKey: 'detergents', featured: true, stock: 68, retail: 780, wholesale: 640, business: 585 },
  { sku: 'CS-QAT-20L', slug: 'quaternary-hospital-sanitizer-20l', category: 'industrial-chemicals', nameAr: 'معقم رباعي الأمونيوم للمستشفيات 20 لتر', nameEn: 'Quaternary Hospital Sanitizer 20L', descriptionAr: 'معقم مركز للاستخدامات المهنية والطبية.', descriptionEn: 'Concentrated quaternary sanitizer for professional healthcare use.', brand: 'SterilGuard', unitLabelAr: 'بستلة 20 لتر', unitLabelEn: '20L Drum', baseUnit: 'DRUM', cartonSize: 1, bulkMinQty: 4, bulkDiscountPercent: 15, moqRetail: 1, moqWholesale: 2, moqBusiness: 2, imageKey: 'detergents', featured: false, stock: 52, retail: 920, wholesale: 760, business: 690 },
] as const;

async function seedCatalog(): Promise<void> {
  const categoryIds = new Map<string, string>();
  for (const category of categories) {
    const saved = await prisma.category.upsert({
      where: { slug: category.slug },
      update: { nameAr: category.nameAr, nameEn: category.nameEn, iconKey: category.iconKey, sortOrder: category.sortOrder, isActive: true },
      create: category,
    });
    categoryIds.set(category.slug, saved.id);
  }

  const warehouse = await prisma.warehouse.upsert({
    where: { code: 'CENTRAL' },
    update: {},
    create: { code: 'CENTRAL', name: 'Central Warehouse', isActive: true },
  });

  for (const fixture of products) {
    const categoryId = categoryIds.get(fixture.category);
    if (!categoryId) throw new Error(`Missing category for product ${fixture.sku}`);

    const product = await prisma.product.upsert({
      where: { sku: fixture.sku },
      update: {
        slug: fixture.slug, categoryId, nameAr: fixture.nameAr, nameEn: fixture.nameEn,
        descriptionAr: fixture.descriptionAr, descriptionEn: fixture.descriptionEn,
        brand: fixture.brand, unitLabelAr: fixture.unitLabelAr, unitLabelEn: fixture.unitLabelEn,
        baseUnit: fixture.baseUnit, cartonSize: fixture.cartonSize,
        bulkMinQty: fixture.bulkMinQty, bulkDiscountPercent: fixture.bulkDiscountPercent,
        moqRetail: fixture.moqRetail, moqWholesale: fixture.moqWholesale, moqBusiness: fixture.moqBusiness,
        imageKey: fixture.imageKey, featured: fixture.featured, isActive: true,
      },
      create: {
        sku: fixture.sku, slug: fixture.slug, categoryId, nameAr: fixture.nameAr, nameEn: fixture.nameEn,
        descriptionAr: fixture.descriptionAr, descriptionEn: fixture.descriptionEn,
        brand: fixture.brand, unitLabelAr: fixture.unitLabelAr, unitLabelEn: fixture.unitLabelEn,
        baseUnit: fixture.baseUnit, cartonSize: fixture.cartonSize,
        bulkMinQty: fixture.bulkMinQty, bulkDiscountPercent: fixture.bulkDiscountPercent,
        moqRetail: fixture.moqRetail, moqWholesale: fixture.moqWholesale, moqBusiness: fixture.moqBusiness,
        imageKey: fixture.imageKey, featured: fixture.featured, isActive: true,
      },
    });

    const priceRows = [
      { segment: CustomerSegment.RETAIL, tier: CustomerTier.STANDARD, unitPrice: fixture.retail },
      { segment: CustomerSegment.WHOLESALE, tier: CustomerTier.STANDARD, unitPrice: fixture.wholesale },
      { segment: CustomerSegment.BUSINESS, tier: CustomerTier.STANDARD, unitPrice: fixture.business },
    ];
    for (const price of priceRows) {
      const existing = await prisma.productPrice.findFirst({
        where: { productId: product.id, segment: price.segment, tier: price.tier, validUntil: null },
      });
      if (!existing) {
        await prisma.productPrice.create({ data: { productId: product.id, ...price, currency: 'EGP' } });
      }
    }

    // Demo stock is inserted only for a new balance; rerunning seed never resets live stock.
    await prisma.inventoryBalance.createMany({
      data: [{ warehouseId: warehouse.id, productId: product.id, quantity: fixture.stock, reservedQuantity: 0 }],
      skipDuplicates: true,
    });
  }
}

async function main(): Promise<void> {
  const roles = new Map<string, string>();
  for (const definition of roleDefinitions) {
    const role = await prisma.role.upsert({
      where: { code: definition.code },
      update: { name: definition.name, description: definition.description },
      create: definition,
    });
    roles.set(definition.code, role.id);
  }

  const permissions = new Map<string, string>();
  for (const code of permissionDefinitions) {
    const permission = await prisma.permission.upsert({
      where: { code },
      update: {},
      create: { code, description: code.replaceAll('.', ' ') },
    });
    permissions.set(code, permission.id);
  }

  for (const [roleCode, codes] of Object.entries(rolePermissions)) {
    const roleId = roles.get(roleCode);
    if (!roleId) throw new Error(`Missing seeded role: ${roleCode}`);
    for (const permissionCode of codes) {
      const permissionId = permissions.get(permissionCode);
      if (!permissionId) throw new Error(`Missing permission: ${permissionCode}`);
      await prisma.rolePermission.upsert({
        where: { roleId_permissionId: { roleId, permissionId } },
        update: {},
        create: { roleId, permissionId },
      });
    }
  }

  await seedCatalog();
}

main()
  .catch((error: unknown) => {
    console.error('CleanSupply seed failed', error);
    process.exitCode = 1;
  })
  .finally(async () => {
    await prisma.$disconnect();
  });
