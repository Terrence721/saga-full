import { useEffect, useRef, useState, type FormEvent } from 'react'
import { useQuery } from '@tanstack/react-query'
import { createOrder, streamOrder, type Order } from '../api/orderClient'
import { ApiError } from '../api/httpClient'
import { fetchMenu, type MenuItem } from '../api/menuClient'
import { MENU } from '../data/menu'

export interface CartLine {
  item: MenuItem
  quantity: number
}

export function useOrderEntry() {
  const menuQuery = useQuery({
    queryKey: ['menu'],
    queryFn: () => fetchMenu(),
  })
  const [itemCode, setItemCode] = useState('')
  const [quantity, setQuantity] = useState('1')
  const [totalAmount, setTotalAmount] = useState('')
  const [tableNumber, setTableNumber] = useState('')
  const [customerName, setCustomerName] = useState('')
  const [paymentMethod, setPaymentMethod] = useState<'CASH' | 'CARD'>('CASH')
  const [cardNumber, setCardNumber] = useState('')
  const [cardCvv, setCardCvv] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [cart, setCart] = useState<CartLine[]>([])
  const [placed, setPlaced] = useState<Order[]>([])
  const [menu, setMenu] = useState<MenuItem[]>(() => MENU.map((item) => ({ ...item, available: true })))
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [order, setOrder] = useState<Order | null>(null)
  const stopStreamRef = useRef<(() => void) | null>(null)

  useEffect(() => () => stopStreamRef.current?.(), [])

  useEffect(() => {
    if (menuQuery.data && menuQuery.data.length) setMenu(menuQuery.data)
  }, [menuQuery.data])

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    setIsSubmitting(true)

    const customerId = 'guest'

    const lines = cart.length > 0
      ? cart
      : [{ item: { itemCode, name: itemCode, description: '', category: itemCode, price: Number(totalAmount) || 0, available: true }, quantity: Number(quantity) || 1 }]

    try {
      const createdOrders: Order[] = []
      for (const line of lines) {
        const created = await createOrder({
          customerId,
          itemCode: line.item.itemCode,
          quantity: line.quantity,
          totalAmount: line.item.price * line.quantity,
          tableNumber: tableNumber || null,
          customerName: customerName || null,
          paymentMethod,
          cardNumber: paymentMethod === 'CARD' ? (cardNumber || null) : null,
          cardCvv: paymentMethod === 'CARD' ? (cardCvv || null) : null,
        })
        createdOrders.push(created)
        stopStreamRef.current = streamOrder(created.id, (updated) => {
          setPlaced((current) => current.map((row) => (row.id === updated.id ? updated : row)))
        })
      }
      setPlaced(createdOrders)
      setCart([])
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Order could not be created')
    } finally {
      setIsSubmitting(false)
    }
  }

  function startNewOrder() {
    stopStreamRef.current?.()
    stopStreamRef.current = null
    setOrder(null)
  }

  return {
    stopStreamRef,
    itemCode, setItemCode,
    quantity, setQuantity,
    totalAmount, setTotalAmount,
    tableNumber, setTableNumber,
    customerName, setCustomerName,
    paymentMethod, setPaymentMethod,
    cardNumber, setCardNumber,
    cardCvv, setCardCvv,
    error, setError,
    cart, setCart,
    placed, setPlaced,
    menu, setMenu,
    isSubmitting,
    order, setOrder,
    handleSubmit,
    startNewOrder,
  }
}
