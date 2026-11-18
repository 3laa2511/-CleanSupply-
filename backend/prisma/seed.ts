import {
  CustomerSegment,
  PrismaClient,
} from '@prisma/client';

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
  'dashboard.read',
  'users.read',
  'users.manage',
  'catalog.read',
  'catalog.manage',
  'pricing.read',
  'pricing.manage',
  'inventory.read',
  'inventory.manage',
  'purchasing.read',
  'purchasing.manage',
  'orders.read',
  'orders.manage',
  'payments.read',
  'payments.reconcile',
  'credit.manage',
  'delivery.read',
  'delivery.manage',
  'support.read',
  'support.manage',
  'audit.read',
  'settings.manage',
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

  await prisma.warehouse.upsert({
    where: { code: 'CENTRAL' },
    update: {},
    create: { code: 'CENTRAL', name: 'Central Warehouse', isActive: true },
  });

  // CustomerSegment is imported intentionally to keep generated Prisma enum
  // availability checked during seed compilation; no customer is seeded.
  void CustomerSegment.RETAIL;
}

main()
  .catch((error: unknown) => {
    console.error('CleanSupply seed failed', error);
    process.exitCode = 1;
  })
  .finally(async () => {
    await prisma.$disconnect();
  });
