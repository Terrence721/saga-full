// Seeded register menu. Prices are authoritative for the POS front-end; the
// backing inventory (`restaurant_inventory`) only tracks stock counts, so the
// unit price lives here and feeds `totalAmount` automatically on selection.
export interface MenuItem {
  itemCode: string
  name: string
  description: string
  category: string
  price: number
  spicy?: boolean
}

export const MENU: MenuItem[] = [
  { itemCode: 'MARGHERITA', name: 'Margherita', description: 'Tomato, mozzarella, basil', category: 'MARGHERITA', price: 8.5 },
  { itemCode: 'PEPPERONI', name: 'Pepperoni', description: 'Tomato, mozzarella, pepperoni', category: 'PEPPERONI', price: 10.0 },
  { itemCode: 'HEAT_WAVE', name: 'Heat Wave', description: 'Spicy salami, jalapeño, chili oil', category: 'HEAT_WAVE', price: 11.5, spicy: true },
  { itemCode: 'QUATTRO_FORMAGGI', name: 'Quattro Formaggi', description: 'Mozzarella, gorgonzola, fontina, parmesan', category: 'QUATTRO_FORMAGGI', price: 12.0 },
  { itemCode: 'VEGGIE', name: 'Veggie', description: 'Artichoke, mushroom, bell pepper, olive', category: 'VEGGIE', price: 11.0 },
  { itemCode: 'CAPRICCIOSA', name: 'Capricciosa', description: 'Ham, artichoke, mushroom, olive, egg', category: 'CAPRICCIOSA', price: 13.0 },
  { itemCode: 'DIAVOLA', name: 'Diavola', description: 'Spicy nduja, mozzarella, chili', category: 'DIAVOLA', price: 12.5, spicy: true },
  { itemCode: 'TRUFFLE', name: 'Truffle', description: 'Mushroom, truffle oil, parmesan (low stock)', category: 'TRUFFLE', price: 14.0 },
  { itemCode: 'HAWAIIAN', name: 'Hawaiian', description: 'Ham, pineapple, mozzarella', category: 'HAWAIIAN', price: 10.5 },
  { itemCode: 'SPINACH_RICOTTA', name: 'Spinach Ricotta', description: 'Ricotta, spinach, garlic, parmesan', category: 'SPINACH_RICOTTA', price: 11.5 },
  { itemCode: 'COLA', name: 'Cola', description: '330 ml', category: 'COLA', price: 2.5 },
  { itemCode: 'LEMONADE', name: 'Lemonade', description: '330 ml', category: 'LEMONADE', price: 2.5 },
  { itemCode: 'GARLIC_BREAD', name: 'Garlic Bread', description: '4 pieces', category: 'GARLIC_BREAD', price: 4.0 },
]

export function findMenuItem(code: string): MenuItem | undefined {
  return MENU.find((item) => item.itemCode === code)
}

export function categoryItems(category: MenuItem['category']): MenuItem[] {
  return MENU.filter((item) => item.category === category)
}
