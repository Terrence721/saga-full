import '@testing-library/jest-dom/vitest'
import { afterEach } from 'vitest'
import { cleanup } from '@testing-library/react'

// RTL's own auto-cleanup relies on detecting the test framework's global afterEach,
// which isn't registered here on purpose (explicit imports everywhere, no `globals:
// true`) - so it needs wiring up by hand, or every test after the first in a file
// would see every previous test's still-rendered DOM.
afterEach(() => {
  cleanup()
})
