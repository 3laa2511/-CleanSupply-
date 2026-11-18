import { plainToInstance } from 'class-transformer';
import { validate } from 'class-validator';
import { CatalogQueryDto } from './catalog-query.dto';

describe('CatalogQueryDto', () => {
  it.each([
    ['true', true],
    ['false', false],
  ])('transforms featured=%s to a boolean', async (input, expected) => {
    const dto = plainToInstance(CatalogQueryDto, { featured: input });
    expect(dto.featured).toBe(expected);
    await expect(validate(dto)).resolves.toHaveLength(0);
  });

  it('rejects malformed featured values instead of silently treating them as false', async () => {
    const dto = plainToInstance(CatalogQueryDto, { featured: 'yes' });
    const errors = await validate(dto);
    expect(errors.some((error) => error.property === 'featured')).toBe(true);
  });
});
