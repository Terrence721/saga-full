import './landing.css'
import { useEffect, useRef, useState, type ReactNode } from "react"

type MenuItem = {
  id: string
  name: string
  description: string
  price: number
  category: "Pizza" | "Sides" | "Drinks"
  tag?: string
  image?: string
}

const menu: MenuItem[] = [
  {
    id: "margherita",
    name: "Margherita",
    description:
      "San Marzano tomato, fior di latte, basil, extra virgin olive oil",
    price: 12.5,
    category: "Pizza",
    tag: "House favorite",
    image:
      "https://images.unsplash.com/photo-1574071318508-1cdbab80d002?auto=format&fit=crop&w=900&q=85",
  },
  {
    id: "diavola",
    name: "Diavola",
    description: "Spicy nduja, soppressata, mozzarella, chilli honey",
    price: 14,
    category: "Pizza",
    tag: "Spicy",
    image:
      "https://images.unsplash.com/photo-1579751626657-72bc17010498?auto=format&fit=crop&w=900&q=85",
  },
  {
    id: "truffle",
    name: "Wild Truffle",
    description: "Roasted mushroom, truffle cream, parmesan, thyme",
    price: 16,
    category: "Pizza",
    image:
      "https://images.unsplash.com/photo-1598023696416-0193a0bcd302?auto=format&fit=crop&w=900&q=85",
  },
  {
    id: "capricciosa",
    name: "Capricciosa",
    description: "Ham, artichoke, olive, mushroom, fior di latte",
    price: 15,
    category: "Pizza",
  },
  {
    id: "garlic",
    name: "Wood-fired Garlic Bread",
    description: "Confit garlic, rosemary butter, sea salt",
    price: 6,
    category: "Sides",
    tag: "For sharing",
  },
  {
    id: "salad",
    name: "Bitter Leaf Salad",
    description: "Radicchio, rocket, pear, walnut, aged balsamic",
    price: 8.5,
    category: "Sides",
  },
  {
    id: "lemonade",
    name: "House Lemonade",
    description: "Fresh lemon, rosemary syrup, sparkling water",
    price: 4.5,
    category: "Drinks",
  },
  {
    id: "cola",
    name: "Italian Cola",
    description: "Baladin cola, 330 ml",
    price: 4,
    category: "Drinks",
  },
]

const categories = ["All", "Pizza", "Sides", "Drinks"] as const

function ArrowIcon() {
  return (
    <svg viewBox="0 0 20 20" aria-hidden="true">
      <path d="M4 10h12M11 5l5 5-5 5" />
    </svg>
  )
}

function CloseIcon() {
  return (
    <svg viewBox="0 0 20 20" aria-hidden="true">
      <path d="m5 5 10 10M15 5 5 15" />
    </svg>
  )
}

function Dialog({
  children,
  labelledBy,
  onClose,
  size = "wide",
}: {
  children: ReactNode
  labelledBy: string
  onClose: () => void
  size?: "wide" | "narrow"
}) {
  const closeButtonRef = useRef<HTMLButtonElement>(null)
  const onCloseRef = useRef(onClose)

  useEffect(() => {
    onCloseRef.current = onClose
  }, [onClose])

  useEffect(() => {
    const previousOverflow = document.body.style.overflow
    document.body.style.overflow = "hidden"
    closeButtonRef.current?.focus()

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") onCloseRef.current()
    }

    window.addEventListener("keydown", handleKeyDown)
    return () => {
      document.body.style.overflow = previousOverflow
      window.removeEventListener("keydown", handleKeyDown)
    }
  }, [])

  return (
    <div
      className="dialog-backdrop"
      role="presentation"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget) onClose()
      }}
    >
      <section
        className={`dialog-panel dialog-panel--${size}`}
        role="dialog"
        aria-modal="true"
        aria-labelledby={labelledBy}
      >
        <button
          className="dialog-close"
          type="button"
          onClick={onClose}
          aria-label="Close dialog"
          ref={closeButtonRef}
        >
          <CloseIcon />
        </button>
        {children}
      </section>
    </div>
  )
}

const formatPrice = (price: number) =>
  new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: "USD",
  }).format(price)

const tomorrow = new Date(Date.now() + 24 * 60 * 60 * 1000)
  .toISOString()
  .slice(0, 10)

export default function App() {
  const [category, setCategory] = useState<typeof categories[number]>("All")
  const [showOrder, setShowOrder] = useState(false)
  const [showReservation, setShowReservation] = useState(false)
  const [orderComplete, setOrderComplete] = useState(false)
  const [reservationComplete, setReservationComplete] = useState(false)
  const [pickupName, setPickupName] = useState("")
  const [paymentMethod, setPaymentMethod] = useState<"pickup" | "card">(
    "pickup",
  )
  const [cardNumber, setCardNumber] = useState("")
  const [cardExpiry, setCardExpiry] = useState("")
  const [cardCvv, setCardCvv] = useState("")
  const [cart, setCart] = useState<{ item: MenuItem; quantity: number }[]>([])
  const [reservation, setReservation] = useState({
    name: "",
    email: "",
    guests: "2",
    date: tomorrow,
    time: "18:30",
  })
  const visibleMenu =
    category === "All"
      ? menu
      : menu.filter((item) => item.category === category)
  const cartCount = cart.reduce((total, line) => total + line.quantity, 0)
  const cartTotal = cart.reduce(
    (total, line) => total + line.item.price * line.quantity,
    0,
  )
  const cardIsComplete =
    cardNumber.replace(/\s/g, "").length >= 15 &&
    cardExpiry.length === 5 &&
    cardCvv.length >= 3

  const addToCart = (item: MenuItem) => {
    setCart((current) => {
      const existing = current.find((line) => line.item.id === item.id)
      if (existing) {
        return current.map((line) =>
          line.item.id === item.id
            ? { ...line, quantity: line.quantity + 1 }
            : line,
        )
      }
      return [...current, { item, quantity: 1 }]
    })
    setOrderComplete(false)
  }

  const updateQuantity = (itemId: string, difference: number) => {
    setCart((current) =>
      current
        .map((line) =>
          line.item.id === itemId
            ? { ...line, quantity: line.quantity + difference }
            : line,
        )
        .filter((line) => line.quantity > 0),
    )
  }

  return (
    <main>
      <header className="site-header">
        <a className="brand" href="#top" aria-label="Forno home">
          Forno<span>.</span>
        </a>
        <nav aria-label="Primary navigation">
          <a href="#story">Our story</a>
          <a href="#menu">Menu</a>
          <a href="#visit">Visit</a>
        </nav>
        <button
          className="header-order"
          type="button"
          onClick={() => setShowOrder(true)}
        >
          Order online
          {cartCount > 0 && <span className="cart-count">{cartCount}</span>}
          <ArrowIcon />
        </button>
      </header>

      <section className="hero" id="top">
        <div className="hero-copy">
          <p className="eyebrow">Wood-fired in the neighbourhood</p>
          <h1>
            Pizza made
            <br />
            with <em>patience.</em>
          </h1>
          <p className="hero-intro">
            Slow-fermented dough, honest ingredients and a roaring wood fire.
            Made by hand, served without fuss.
          </p>
          <div className="hero-actions">
            <a className="primary-button" href="#menu">
              Explore our menu <ArrowIcon />
            </a>
            <button
              className="text-button"
              type="button"
              onClick={() => setShowOrder(true)}
            >
              Order for pickup <ArrowIcon />
            </button>
            <div className="opening-note">
              <span className="status-dot" />
              <span>
                Open today
                <strong>12:00 — 22:30</strong>
              </span>
            </div>
          </div>
        </div>

        <div className="hero-visual">
          <img
            src="https://images.unsplash.com/photo-1571997478779-2adcbbe9ab2f?auto=format&fit=crop&w=1400&q=90"
            alt="Two freshly baked Neapolitan pizzas on a table"
          />
          <div className="hero-stamp" aria-hidden="true">
            <span>48</span>
            <small>hour dough</small>
          </div>
          <div className="hero-caption">
            <span>01</span>
            <p>
              Charred, chewy,
              <br />
              unmistakably ours.
            </p>
          </div>
        </div>
      </section>

      <section className="marquee" aria-label="Our values">
        <div>
          <span>Hand stretched</span>
          <i />
          <span>Local ingredients</span>
          <i />
          <span>Fired at 450°</span>
          <i />
          <span>Made to order</span>
          <i />
          <span>Hand stretched</span>
        </div>
      </section>

      <section className="story" id="story">
        <p className="section-number">01 — Our approach</p>
        <blockquote>
          “We make the kind of pizza we want to eat: simple, blistered, and full
          of life.”
        </blockquote>
        <p className="story-copy">
          Our dough starts two days before it reaches your plate. That time
          gives every crust its lightness, tang, and signature chew. We source
          carefully and let every ingredient speak for itself.
        </p>
      </section>

      <section className="menu-section" id="menu">
        <div className="menu-heading">
          <div>
            <p className="eyebrow">From our oven</p>
            <h2>Choose your favourite.</h2>
          </div>
          <div
            className="category-tabs"
            role="tablist"
            aria-label="Menu categories"
          >
            {categories.map((item) => (
              <button
                key={item}
                role="tab"
                aria-selected={category === item}
                className={category === item ? "active" : ""}
                onClick={() => setCategory(item)}
              >
                {item}
              </button>
            ))}
          </div>
        </div>

        <div className="menu-grid landing-menu-grid" role="tabpanel">
          {visibleMenu.map((item, index) => (
            <article
              className={`menu-card ${item.image ? "with-image" : ""}`}
              key={item.id}
              style={{ animationDelay: `${index * 55}ms` }}
            >
              {item.image && <img src={item.image} alt="" />}
              <div className="menu-card-body">
                <div className="item-meta">
                  <span>{item.category}</span>
                  {item.tag && <span className="item-tag">{item.tag}</span>}
                </div>
                <div className="item-title">
                  <h3>{item.name}</h3>
                  <strong>{formatPrice(item.price)}</strong>
                </div>
                <p>{item.description}</p>
                <button type="button" onClick={() => addToCart(item)}>
                  Add to pickup order
                  <span aria-hidden="true">+</span>
                </button>
              </div>
            </article>
          ))}
        </div>
      </section>

      <section className="visit-section" id="visit">
        <div className="visit-image">
          <img
            src="https://images.unsplash.com/photo-1622880833523-7cf1c0bd4296?auto=format&fit=crop&w=1400&q=85"
            alt="Pizza baking in a wood-fired oven"
          />
        </div>
        <div className="visit-copy">
          <p className="eyebrow">Come by tonight</p>
          <h2>A seat by the oven is waiting.</h2>
          <p>
            Walk-ins are always welcome. For groups of six or more, give us a
            call and we’ll save you a table.
          </p>
          <div className="visit-details">
            <p>
              <span>Find us</span>
              12345 Market Lane
              <br />
              Southfield, Michigan
            </p>
            <p>
              <span>Opening hours</span>
              Tuesday–Sunday
              <br />
              12:00 PM–10:30 PM
            </p>
          </div>
          <a className="primary-button" href="tel:+12481234567">
            Call 248-123-4567 <ArrowIcon />
          </a>
          <button
            className="text-button visit-reserve"
            type="button"
            onClick={() => {
              setReservationComplete(false)
              setShowReservation(true)
            }}
          >
            Request a table <ArrowIcon />
          </button>
        </div>
      </section>

      <footer>
        <div>
          <a className="brand footer-brand" href="#top">
            Forno<span>.</span>
          </a>
          <p>Good dough. Great fire. No shortcuts.</p>
        </div>
        <div className="footer-links">
          <a href="#story">Our story</a>
          <a href="#menu">Menu</a>
          <a href="#visit">Visit</a>
        </div>
        <div className="footer-bottom">
          <p>© 2026 Forno Pizza Co.</p>
          <a
            href="https://unsplash.com/s/photos/neapolitan-pizza"
            target="_blank"
            rel="noreferrer"
          >
            Photography via Unsplash
          </a>
        </div>
      </footer>

      {showOrder && (
        <Dialog labelledBy="order-title" onClose={() => setShowOrder(false)}>
          {orderComplete ? (
            <div className="dialog-success" aria-live="polite">
              <span className="success-mark" aria-hidden="true">
                ✓
              </span>
              <p className="eyebrow">Order received</p>
              <h2 id="order-title">We’ll start the oven.</h2>
              <p>
                Thanks, {pickupName}. Your pickup order will be ready in around
                20 minutes.{" "}
                {paymentMethod === "card"
                  ? "Your demo card details were accepted locally."
                  : "Payment is due when you collect."}
              </p>
              <button
                className="primary-button"
                type="button"
                onClick={() => setShowOrder(false)}
              >
                Done
              </button>
            </div>
          ) : (
            <>
              <div className="dialog-heading">
                <p className="eyebrow">Pickup only · About 20 min</p>
                <h2 id="order-title">Order online.</h2>
                <p>Choose your favourites, then pay when you collect.</p>
              </div>
              <div className="order-dialog-layout">
                <div
                  className="order-menu"
                  aria-label="Items available to order"
                >
                  {menu.map((item) => {
                    const quantity =
                      cart.find((line) => line.item.id === item.id)?.quantity ??
                      0
                    return (
                      <article className="order-menu-item" key={item.id}>
                        {item.image ? (
                          <img src={item.image} alt="" />
                        ) : (
                          <div
                            className="order-menu-placeholder"
                            aria-hidden="true"
                          >
                            Forno.
                          </div>
                        )}
                        <div>
                          <span>{item.category}</span>
                          <h3>{item.name}</h3>
                          <p>{item.description}</p>
                          <strong>{formatPrice(item.price)}</strong>
                        </div>
                        {quantity === 0 ? (
                          <button
                            className="add-button"
                            type="button"
                            onClick={() => addToCart(item)}
                            aria-label={`Add ${item.name} to order`}
                          >
                            Add
                          </button>
                        ) : (
                          <div
                            className="quantity-control"
                            aria-label={`${item.name} quantity`}
                          >
                            <button
                              type="button"
                              onClick={() => updateQuantity(item.id, -1)}
                              aria-label={`Remove one ${item.name}`}
                            >
                              −
                            </button>
                            <span>{quantity}</span>
                            <button
                              type="button"
                              onClick={() => updateQuantity(item.id, 1)}
                              aria-label={`Add one ${item.name}`}
                            >
                              +
                            </button>
                          </div>
                        )}
                      </article>
                    )
                  })}
                </div>

                <aside className="cart-summary">
                  <div className="cart-summary-heading">
                    <h3>Your pickup</h3>
                    <span>
                      {cartCount} {cartCount === 1 ? "item" : "items"}
                    </span>
                  </div>
                  {cart.length === 0 ? (
                    <div className="empty-cart">
                      <p>Your cart is empty.</p>
                      <span>Add an item to get started.</span>
                    </div>
                  ) : (
                    <div className="cart-lines" aria-live="polite">
                      {cart.map((line) => (
                        <div className="cart-line" key={line.item.id}>
                          <span>
                            {line.quantity} × {line.item.name}
                          </span>
                          <strong>
                            {formatPrice(line.item.price * line.quantity)}
                          </strong>
                        </div>
                      ))}
                    </div>
                  )}
                  <div className="cart-total">
                    <span>Total</span>
                    <strong>{formatPrice(cartTotal)}</strong>
                  </div>
                  <label className="dialog-field">
                    <span>Name for pickup</span>
                    <input
                      name="pickupName"
                      value={pickupName}
                      onChange={(event) => setPickupName(event.target.value)}
                      placeholder="Your name"
                      autoComplete="name"
                    />
                  </label>
                  <fieldset className="payment-options">
                    <legend>Payment method</legend>
                    <div className="payment-choice-grid">
                      <label
                        className={
                          paymentMethod === "pickup"
                            ? "payment-choice payment-choice--active"
                            : "payment-choice"
                        }
                      >
                        <input
                          type="radio"
                          name="payment-method"
                          checked={paymentMethod === "pickup"}
                          onChange={() => setPaymentMethod("pickup")}
                        />
                        <span>
                          <strong>Pay at pickup</strong>
                          <small>Cash or card at the counter</small>
                        </span>
                      </label>
                      <label
                        className={
                          paymentMethod === "card"
                            ? "payment-choice payment-choice--active"
                            : "payment-choice"
                        }
                      >
                        <input
                          type="radio"
                          name="payment-method"
                          checked={paymentMethod === "card"}
                          onChange={() => setPaymentMethod("card")}
                        />
                        <span>
                          <strong>Card online</strong>
                          <small>Demo input only</small>
                        </span>
                      </label>
                    </div>
                  </fieldset>
                  {paymentMethod === "card" && (
                    <div className="card-fields">
                      <label className="dialog-field">
                        <span>Card number</span>
                        <input
                          inputMode="numeric"
                          autoComplete="cc-number"
                          placeholder="1234 5678 9012 3456"
                          value={cardNumber}
                          maxLength={19}
                          onChange={(event) => {
                            const digits = event.target.value
                              .replace(/\D/g, "")
                              .slice(0, 16)
                            setCardNumber(
                              digits.replace(/(\d{4})(?=\d)/g, "$1 "),
                            )
                          }}
                        />
                      </label>
                      <div className="card-field-row">
                        <label className="dialog-field">
                          <span>Expiry</span>
                          <input
                            name="cardExpiry"
                            inputMode="numeric"
                            autoComplete="cc-exp"
                            placeholder="MM/YY"
                            value={cardExpiry}
                            maxLength={5}
                            onChange={(event) => {
                              const digits = event.target.value
                                .replace(/\D/g, "")
                                .slice(0, 4)
                              setCardExpiry(
                                digits.length > 2
                                  ? `${digits.slice(0, 2)}/${digits.slice(2)}`
                                  : digits,
                              )
                            }}
                          />
                        </label>
                        <label className="dialog-field">
                          <span>Security code</span>
                          <input
                            name="cardCvv"
                            type="password"
                            inputMode="numeric"
                            autoComplete="cc-csc"
                            placeholder="CVV"
                            value={cardCvv}
                            maxLength={4}
                            onChange={(event) =>
                              setCardCvv(
                                event.target.value
                                  .replace(/\D/g, "")
                                  .slice(0, 4),
                              )
                            }
                          />
                        </label>
                      </div>
                      <p className="card-disclaimer">
                        Demo only — card details stay in this browser and are
                        cleared after checkout. No charge will be made.
                      </p>
                    </div>
                  )}
                  <button
                    className="checkout-button"
                    type="button"
                    disabled={
                      cart.length === 0 ||
                      !pickupName.trim() ||
                      (paymentMethod === "card" && !cardIsComplete)
                    }
                    onClick={() => {
                      setOrderComplete(true)
                      setCart([])
                      setCardNumber("")
                      setCardExpiry("")
                      setCardCvv("")
                    }}
                  >
                    Place pickup order <ArrowIcon />
                  </button>
                  <p className="cart-note">
                    {paymentMethod === "pickup"
                      ? "No card needed. Pay at the restaurant when you collect."
                      : "Your card is not charged in this frontend-only demo."}
                  </p>
                </aside>
              </div>
            </>
          )}
        </Dialog>
      )}

      {showReservation && (
        <Dialog
          labelledBy="reservation-title"
          onClose={() => setShowReservation(false)}
          size="narrow"
        >
          {reservationComplete ? (
            <div className="dialog-success" aria-live="polite">
              <span className="success-mark" aria-hidden="true">
                ✓
              </span>
              <p className="eyebrow">Request received</p>
              <h2 id="reservation-title">We’ll be in touch.</h2>
              <p>
                Your table request for {reservation.guests} on{" "}
                {new Date(`${reservation.date}T12:00`).toLocaleDateString(
                  "en-US",
                  { month: "long", day: "numeric" },
                )}{" "}
                at {reservation.time} is ready for confirmation.
              </p>
              <button
                className="primary-button"
                type="button"
                onClick={() => setShowReservation(false)}
              >
                Done
              </button>
            </div>
          ) : (
            <form
              className="reservation-form"
              onSubmit={(event) => {
                event.preventDefault()
                setReservationComplete(true)
              }}
            >
              <div className="dialog-heading">
                <p className="eyebrow">Plan your visit</p>
                <h2 id="reservation-title">Request a table.</h2>
                <p>We’ll contact you to confirm availability.</p>
              </div>
              <label className="dialog-field">
                <span>Name</span>
                <input
                  name="reservationName"
                  required
                  value={reservation.name}
                  onChange={(event) =>
                    setReservation((current) => ({
                      ...current,
                      name: event.target.value,
                    }))
                  }
                  autoComplete="name"
                  placeholder="Your full name"
                />
              </label>
              <label className="dialog-field">
                <span>Email</span>
                <input
                  name="reservationEmail"
                  required
                  type="email"
                  value={reservation.email}
                  onChange={(event) =>
                    setReservation((current) => ({
                      ...current,
                      email: event.target.value,
                    }))
                  }
                  autoComplete="email"
                  placeholder="you@example.com"
                />
              </label>
              <div className="form-row">
                <label className="dialog-field">
                  <span>Party size</span>
                  <select
                    name="partySize"
                    value={reservation.guests}
                    onChange={(event) =>
                      setReservation((current) => ({
                        ...current,
                        guests: event.target.value,
                      }))
                    }
                  >
                    {[1, 2, 3, 4, 5, 6, 7, 8].map((count) => (
                      <option value={count} key={count}>
                        {count} {count === 1 ? "guest" : "guests"}
                      </option>
                    ))}
                  </select>
                </label>
                <label className="dialog-field">
                  <span>Time</span>
                  <select
                    name="reservationTime"
                    value={reservation.time}
                    onChange={(event) =>
                      setReservation((current) => ({
                        ...current,
                        time: event.target.value,
                      }))
                    }
                  >
                    {[
                      '5:00 PM',
                      '5:30 PM',
                      '6:00 PM',
                      '6:30 PM',
                      '7:00 PM',
                      '7:30 PM',
                      '8:00 PM',
                      '8:30 PM',
                    ].map((time) => (
                      <option value={time} key={time}>
                        {time}
                      </option>
                    ))}
                  </select>
                </label>
              </div>
              <label className="dialog-field">
                <span>Date</span>
                <input
                  name="reservationDate"
                  required
                  type="date"
                  min={tomorrow}
                  value={reservation.date}
                  onChange={(event) =>
                    setReservation((current) => ({
                      ...current,
                      date: event.target.value,
                    }))
                  }
                />
              </label>
              <button className="checkout-button" type="submit">
                Send table request <ArrowIcon />
              </button>
              <p className="cart-note">
                This demo keeps requests in your current browser session only.
              </p>
            </form>
          )}
        </Dialog>
      )}
    </main>
  )
}
