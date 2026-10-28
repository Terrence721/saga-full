// Seeded register menu. Prices are authoritative for the POS front-end; the
// backing inventory (`restaurant_inventory`) only tracks stock counts, so the
// unit price lives here and feeds `totalAmount` automatically on selection.
export interface MenuItem {
  code: string
  name: string
  description: string
  category: 'Pizza' | 'Drink' | 'Side'
  price: number
  spicy?: boolean
}

export const MENU: MenuItem[] = [
  { code: 'PIZZA_01', name: 'Margherita', description: 'Tomato, mozzarella, basil', category: 'Pizza', price: 8.5 },
  { code: 'PIZZA_02', name: 'Pepperoni', description: 'Tomato, mozzarella, pepperoni', category: 'Pizza', price: 10.0 },
  { code: 'PIZZA_03', name: 'Heat Wave', description: 'Spicy salami, jalapeño, chili oil', category: 'Pizza', price: 11.5, spicy: true },
  { code: 'PIZZA_04', name: 'Quattro Formaggi', description: 'Mozzarella, gorgonzola, fontina, parmesan', category: 'Pizza', price: 12.0 },
  { code: 'PIZZA_05', name: 'Veggie', description: 'Artichoke, mushroom, bell pepper, olive', category: 'Pizza', price: 11.0 },
  { code: 'PIZZA_06', name: 'Capricciosa', description: 'Ham, artichoke, mushroom, olive, egg', category: 'Pizza', price: 13.0 },
  { code: 'PIZZA_07', name: 'Diavola', description: 'Spicy nduja, mozzarella, chili', category: 'Pizza', price: 12.5, spicy: true },
  { code: 'PIZZA_08', name: 'Truffle', description: 'Mushroom, truffle oil, parmesan (low stock)', category: 'Pizza', price: 14.0 },
  { code: 'PIZZA_09', name: 'Hawaiian', description: 'Ham, pineapple, mozzarella', category: 'Pizza', price: 10.5 },
  { code: 'PIZZA_10', name: 'Spinach Ricotta', description: 'Ricotta, spinach, garlic, parmesan', category: 'Pizza', price: 11.5 },
  { code: 'DRINK_01', name: 'Cola', description: '330 ml', category: 'Drink', price: 2.5 },
  { code: 'DRINK_02', name: 'Lemonade', description: '330 ml', category: 'Drink', price: 2.5 },
  { code: 'SIDE_01', name: 'Garlic Bread', description: '4 pieces', category: 'Side', price: 4.0 },
]

export function findMenuItem(code: string): MenuItem | undefined {
  return MENU.find((item) => item.code === code)
}

export function categoryItems(category: MenuItem['category']): MenuItem[] {
  return MENU.filter((item) => item.category === category)
}
