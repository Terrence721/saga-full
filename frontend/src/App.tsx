import './landing.css'
import { useState } from 'react'
import { apiFetch } from './api/httpClient'

type MenuItem = {
  id: string
  name: string
  description: string
  price: number
  category: 'Pizza' | 'Sides' | 'Drinks'
  tag?: string
  image?: string
}

const menu: MenuItem[] = [
  {
    id: 'margherita',
    name: 'Margherita',
    description: 'San Marzano tomato, fior di latte, basil, extra virgin olive oil',
    price: 12.5,
    category: 'Pizza',
    tag: 'House favorite',
    image: 'https://images.unsplash.com/photo-1574071318508-1cdbab80d002?auto=format&fit=crop&w=900&q=85',
  },
  {
    id: 'diavola',
    name: 'Diavola',
    description: 'Spicy nduja, soppressata, mozzarella, chilli honey',
    price: 14,
    category: 'Pizza',
    tag: 'Spicy',
    image: 'https://images.unsplash.com/photo-1579751626657-72bc17010498?auto=format&fit=crop&w=900&q=85',
  },
  {
    id: 'truffle',
    name: 'Wild Truffle',
    description: 'Roasted mushroom, truffle cream, parmesan, thyme',
    price: 16,
    category: 'Pizza',
    image: 'https://images.unsplash.com/photo-1598023696416-0193a0bcd302?auto=format&fit=crop&w=900&q=85',
  },
  {
    id: 'capricciosa',
    name: 'Capricciosa',
    description: 'Ham, artichoke, olive, mushroom, fior di latte',
    price: 15,
    category: 'Pizza',
  },
  {
    id: 'garlic',
    name: 'Wood-fired Garlic Bread',
    description: 'Confit garlic, rosemary butter, sea salt',
    price: 6,
    category: 'Sides',
    tag: 'For sharing',
  },
  {
    id: 'salad',
    name: 'Bitter Leaf Salad',
    description: 'Radicchio, rocket, pear, walnut, aged balsamic',
    price: 8.5,
    category: 'Sides',
  },
  {
    id: 'lemonade',
    name: 'House Lemonade',
    description: 'Fresh lemon, rosemary syrup, sparkling water',
    price: 4.5,
    category: 'Drinks',
  },
  {
    id: 'cola',
    name: 'Italian Cola',
    description: 'Baladin cola, 330 ml',
    price: 4,
    category: 'Drinks',
  },
]

const categories = ['All', 'Pizza', 'Sides', 'Drinks'] as const

function ArrowIcon() {
  return (
    <svg viewBox="0 0 20 20" aria-hidden="true">
      <path d="M4 10h12M11 5l5 5-5 5" />
    </svg>
  )
}

export default function App() {
  const [category, setCategory] = useState<(typeof categories)[number]>('All')
  const [showReservation, setShowReservation] = useState(false)
  const [reservationStatus, setReservationStatus] = useState<string | null>(null)
  const [reservationForm, setReservationForm] = useState({
    customerId: 'guest',
    tableNumber: 'T-1',
    partySize: '2',
    startTime: new Date(Date.now() + 60 * 60 * 1000).toISOString().slice(0, 16),
  })
  const visibleMenu = category === 'All' ? menu : menu.filter((item) => item.category === category)

  async function handleReserveSubmit(event: React.FormEvent) {
    event.preventDefault()
    setReservationStatus(null)
    try {
      const reservation = await apiFetch<{ id: string; status: string }>('/reservations', {
        method: 'POST',
        body: JSON.stringify({
          customerId: reservationForm.customerId,
          tableNumber: reservationForm.tableNumber,
          partySize: Number(reservationForm.partySize),
          startTime: new Date(reservationForm.startTime).toISOString(),
        }),
      })
      setReservationStatus(`Reservation confirmed (${reservation.id})`)
    } catch (error) {
      setReservationStatus(error instanceof Error ? error.message : 'Reservation failed')
    }
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
        <a className="header-order" href="#visit">
          Visit us <ArrowIcon />
        </a>
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
            <div className="opening-note">
              <span className="status-dot" />
              <span>
                Open today
                <strong>12:00 PM — 10:30 PM</strong>
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
          <div className="category-tabs" role="tablist" aria-label="Menu categories">
            {categories.map((item) => (
              <button
                key={item}
                role="tab"
                aria-selected={category === item}
                className={category === item ? 'active' : ''}
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
              className={`menu-card ${item.image ? 'with-image' : ''}`}
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
                  <strong>${item.price.toFixed(2)}</strong>
                </div>
                <p>{item.description}</p>
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
          <button className="primary-button" type="button" onClick={() => setShowReservation(true)}>
            Reserve a table <ArrowIcon />
          </button>
          <a className="primary-button" href="tel:+12481234567">
            Call 248-123-4567 <ArrowIcon />
          </a>
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
        </div>
      </footer>

      {showReservation && (
        <div
          role="dialog"
          aria-modal="true"
          style={{
            position: 'fixed',
            inset: 0,
            background: 'rgba(28,27,24,0.45)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            padding: '1rem',
            zIndex: 100,
          }}
        >
          <form
            onSubmit={handleReserveSubmit}
            style={{
              background: '#fbf8f1',
              border: '1px solid rgba(28,27,24,0.18)',
              padding: '2rem',
              width: 'min(420px, 100%)',
              display: 'grid',
              gap: '0.75rem',
            }}
          >
            <h2 style={{ fontFamily: "'Italiana', serif", fontSize: '2rem', margin: 0 }}>Reserve a table</h2>
            <input
              id="reservation-customer-id"
              name="customerId"
              placeholder="Customer ID"
              value={reservationForm.customerId}
              onChange={(e) => setReservationForm((f) => ({ ...f, customerId: e.target.value }))}
              required
            />
            <input
              id="reservation-table-number"
              name="tableNumber"
              placeholder="Table number (e.g. T-1)"
              value={reservationForm.tableNumber}
              onChange={(e) => setReservationForm((f) => ({ ...f, tableNumber: e.target.value }))}
              required
            />
            <input
              id="reservation-party-size"
              name="partySize"
              type="number"
              min={1}
              placeholder="Party size"
              value={reservationForm.partySize}
              onChange={(e) => setReservationForm((f) => ({ ...f, partySize: e.target.value }))}
              required
            />
            <input
              id="reservation-start-time"
              name="startTime"
              type="datetime-local"
              value={reservationForm.startTime}
              onChange={(e) => setReservationForm((f) => ({ ...f, startTime: e.target.value }))}
              required
            />
            <div style={{ display: 'flex', gap: '0.75rem', justifyContent: 'flex-end', marginTop: '0.5rem' }}>
              <button type="button" onClick={() => setShowReservation(false)}>
                Cancel
              </button>
              <button type="submit" className="primary-button" style={{ margin: 0 }}>
                Confirm
              </button>
            </div>
            {reservationStatus && <p style={{ marginTop: '0.5rem' }}>{reservationStatus}</p>}
          </form>
        </div>
      )}
    </main>
  )
}
