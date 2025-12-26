import React, { useEffect, useMemo, useState } from 'react'
import { useParams } from 'react-router-dom'
import InventoryApi, { type Product, type Stock } from '../api/InventoryApi'

const InventoryPage: React.FC = () => {
  const { branchId } = useParams<{ branchId: string }>()
  const [products, setProducts] = useState<Product[]>([])
  const [stock, setStock] = useState<Stock[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [activeTab, setActiveTab] = useState<'stock' | 'products'>('stock')
  const [showAddProduct, setShowAddProduct] = useState(false)
  const [showAddStock, setShowAddStock] = useState(false)
  const [newProduct, setNewProduct] = useState({ sku: '', name: '', category: '', unit: '' })
  const [newStock, setNewStock] = useState({ productId: 0, quantityOnHand: 0, reorderThreshold: 10 })
  const [countedItems, setCountedItems] = useState<Record<number, boolean>>({})
  
  // Editing state
  const [editingStockId, setEditingStockId] = useState<number | null>(null)
  const [editValues, setEditValues] = useState<{ quantity: number; threshold: number }>({ quantity: 0, threshold: 0 })
  const [saving, setSaving] = useState(false)

  const lowStockItems = useMemo(() => (
    stock.filter((s) => s.quantityOnHand < s.reorderThreshold)
  ), [stock])

  const criticalItems = useMemo(() => (
    lowStockItems.filter((s) => s.quantityOnHand <= s.reorderThreshold * 0.5)
  ), [lowStockItems])

  const warningItems = useMemo(() => (
    lowStockItems.filter((s) => s.quantityOnHand > s.reorderThreshold * 0.5)
  ), [lowStockItems])

  const reorderQueue = useMemo(() => lowStockItems
    .map((item) => {
      const recommendedOrder = Math.max(
        Math.ceil(item.reorderThreshold * 1.5 - item.quantityOnHand),
        0
      )
      const coverageDays = item.reorderThreshold === 0
        ? '∞'
        : Math.max(
            0,
            Math.round(
              (item.quantityOnHand / Math.max(item.reorderThreshold, 1)) * 7
            )
          )

      return {
        id: item.id,
        product: item.product.name,
        sku: item.product.sku,
        category: item.product.category,
        unit: item.product.unit,
        recommendedOrder,
        coverageDays,
        severity: item.quantityOnHand <= item.reorderThreshold * 0.5 ? 'critical' : 'warning'
      }
    })
    .sort((a, b) => b.recommendedOrder - a.recommendedOrder), [lowStockItems])

  const categoryHealth = useMemo(() => {
    const map = new Map<string, { required: number; available: number; lowItems: number }>()
    stock.forEach((item) => {
      const required = Math.max(item.reorderThreshold, 1)
      const available = Math.min(item.quantityOnHand, item.reorderThreshold)
      const entry = map.get(item.product.category) ?? { required: 0, available: 0, lowItems: 0 }
      entry.required += required
      entry.available += available
      if (item.quantityOnHand < item.reorderThreshold) {
        entry.lowItems += 1
      }
      map.set(item.product.category, entry)
    })
    return Array.from(map.entries()).map(([category, metrics]) => ({
      category,
      fillRate: metrics.required === 0 ? 100 : Math.min(100, Math.round((metrics.available / metrics.required) * 100)),
      lowItems: metrics.lowItems,
      totalItems: stock.filter((s) => s.product.category === category).length
    })).sort((a, b) => a.fillRate - b.fillRate)
  }, [stock])

  const todayLabel = useMemo(() => new Intl.DateTimeFormat(undefined, {
    weekday: 'short', month: 'short', day: 'numeric'
  }).format(new Date()), [])

  const checklistItems = useMemo(() => lowStockItems.slice(0, 8), [lowStockItems])
  const countedTotal = useMemo(() => lowStockItems.reduce((sum, item) => sum + (countedItems[item.id] ? 1 : 0), 0), [lowStockItems, countedItems])
  const checklistProgress = lowStockItems.length === 0 ? 100 : Math.round((countedTotal / lowStockItems.length) * 100)
  const reorderTop = useMemo(() => reorderQueue.slice(0, 5), [reorderQueue])
  const coverageScore = categoryHealth.length === 0
    ? 100
    : Math.round(categoryHealth.reduce((sum, entry) => sum + entry.fillRate, 0) / categoryHealth.length)

  useEffect(() => {
    setCountedItems((prev) => {
      const next: Record<number, boolean> = {}
      lowStockItems.forEach((item) => {
        next[item.id] = prev[item.id] ?? false
      })
      return next
    })
  }, [lowStockItems])

  const loadData = async () => {
    if (!branchId) return
    
    setLoading(true)
    setError(null)
    try {
      const [productsData, stockData] = await Promise.all([
        InventoryApi.getProducts(),
        InventoryApi.getBranchStock(Number(branchId))
      ])
      setProducts(productsData)
      setStock(stockData)
    } catch (e: unknown) {
      console.error(e)
      setError('Failed to load inventory data')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadData()
  }, [branchId]) // eslint-disable-line react-hooks/exhaustive-deps

  const handleAddProduct = async () => {
    try {
      await InventoryApi.createProduct(newProduct)
      setNewProduct({ sku: '', name: '', category: '', unit: '' })
      setShowAddProduct(false)
      await loadData()
    } catch (e: unknown) {
      console.error(e)
      setError('Failed to create product')
    }
  }

  // Start editing a stock item
  const handleStartEdit = (item: Stock) => {
    setEditingStockId(item.id)
    setEditValues({
      quantity: item.quantityOnHand,
      threshold: item.reorderThreshold
    })
  }

  // Cancel editing
  const handleCancelEdit = () => {
    setEditingStockId(null)
    setEditValues({ quantity: 0, threshold: 0 })
  }

  // Save edited stock values
  const handleSaveEdit = async (stockId: number) => {
    setSaving(true)
    try {
      await InventoryApi.updateStock(stockId, {
        quantityOnHand: editValues.quantity,
        reorderThreshold: editValues.threshold
      })
      setEditingStockId(null)
      await loadData()
    } catch (e: unknown) {
      console.error(e)
      setError('Failed to update stock')
    } finally {
      setSaving(false)
    }
  }

  // Add stock item for a product that doesn't have one yet
  const handleAddStockItem = async () => {
    if (!branchId || !newStock.productId) return
    
    try {
      await InventoryApi.addStockItem(Number(branchId), newStock)
      setNewStock({ productId: 0, quantityOnHand: 0, reorderThreshold: 10 })
      setShowAddStock(false)
      await loadData()
    } catch (e: unknown) {
      console.error(e)
      setError('Failed to add stock item')
    }
  }

  const toggleChecklistItem = (stockId: number) => {
    setCountedItems((prev) => ({
      ...prev,
      [stockId]: !prev[stockId]
    }))
  }

  const markAllCounted = () => {
    if (lowStockItems.length === 0) return
    setCountedItems((prev) => {
      const next = { ...prev }
      lowStockItems.forEach((item) => {
        next[item.id] = true
      })
      return next
    })
  }

  const resetChecklist = () => {
    setCountedItems((prev) => {
      const next = { ...prev }
      Object.keys(next).forEach((key) => {
        next[Number(key)] = false
      })
      return next
    })
  }

  // Get products that don't have stock entries for this branch yet
  const productsWithoutStock = products.filter(
    p => !stock.some(s => s.product.id === p.id)
  )

  if (loading) {
    return (
      <div className="page">
        <div className="page-header">
          <h2 className="page-title">Inventory</h2>
        </div>
        <div className="card">Loading...</div>
      </div>
    )
  }

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h2 className="page-title">📦 Inventory Management</h2>
          <p className="page-subtitle">
            Manage products and stock levels for Branch {branchId}
          </p>
        </div>
      </div>

      {error && <div className="form-error" style={{ marginBottom: '1rem' }}>{error}</div>}

      {/* Quick Stats */}
      <section style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fit, minmax(140px, 1fr))',
        gap: '1rem',
        marginBottom: '1.5rem'
      }}>
        <div className="card" style={{ textAlign: 'center', padding: '1rem' }}>
          <div style={{ fontSize: '2rem', fontWeight: 700, color: '#4caf50' }}>
            {products.length}
          </div>
          <div style={{ color: '#888', fontSize: '0.9rem' }}>Products</div>
        </div>
        <div className="card" style={{ textAlign: 'center', padding: '1rem' }}>
          <div style={{ fontSize: '2rem', fontWeight: 700, color: '#2196f3' }}>
            {stock.length}
          </div>
          <div style={{ color: '#888', fontSize: '0.9rem' }}>Items in Stock</div>
        </div>
        <div className="card" style={{ textAlign: 'center', padding: '1rem' }}>
          <div style={{ fontSize: '2rem', fontWeight: 700, color: criticalItems.length > 0 ? '#ef4444' : '#4caf50' }}>
            {criticalItems.length}
          </div>
          <div style={{ color: '#888', fontSize: '0.9rem' }}>🚨 Critical</div>
        </div>
        <div className="card" style={{ textAlign: 'center', padding: '1rem' }}>
          <div style={{ fontSize: '2rem', fontWeight: 700, color: warningItems.length > 0 ? '#f59e0b' : '#4caf50' }}>
            {warningItems.length}
          </div>
          <div style={{ color: '#888', fontSize: '0.9rem' }}>⚠️ Warning</div>
        </div>
      </section>

      {/* Low Stock Alerts */}
      {lowStockItems.length > 0 && (
        <div className="card" style={{ 
          marginBottom: '1rem',
          background: 'rgba(255, 152, 0, 0.1)',
          border: '1px solid rgba(255, 152, 0, 0.3)'
        }}>
          <h3 style={{ color: '#ff9800', margin: '0 0 0.75rem 0' }}>
            ⚠️ Low Stock Items
          </h3>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.5rem' }}>
            {lowStockItems.map(item => (
              <span key={item.id} style={{
                padding: '0.35rem 0.75rem',
                borderRadius: '6px',
                background: 'rgba(255, 152, 0, 0.2)',
                color: '#ffa726',
                fontSize: '0.9rem'
              }}>
                {item.product.name}: {item.quantityOnHand}/{item.reorderThreshold} {item.product.unit}
              </span>
            ))}
          </div>
        </div>
      )}

        <section
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))',
            gap: '1rem',
            marginBottom: '1.5rem'
          }}
        >
          <div className="card" style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <div>
                <h3 style={{ margin: 0 }}>Cycle Count Checklist</h3>
                <p style={{ margin: 0, color: '#94a3b8', fontSize: '0.85rem' }}>Due {todayLabel}</p>
              </div>
              <span style={{ fontWeight: 600, color: '#0ea5e9' }}>
                {lowStockItems.length === 0 ? 'All Clear' : `${countedTotal}/${lowStockItems.length}`}
              </span>
            </div>
            <div style={{ height: 6, borderRadius: 999, background: 'rgba(148, 163, 184, 0.3)' }}>
              <div
                style={{
                  height: '100%',
                  width: `${checklistProgress}%`,
                  borderRadius: 999,
                  background: checklistProgress === 100 ? '#22c55e' : '#f97316',
                  transition: 'width 0.2s ease'
                }}
              />
            </div>
            {checklistItems.length === 0 ? (
              <p style={{ margin: 0, color: '#94a3b8' }}>No low stock items require a manual count right now.</p>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.6rem' }}>
                {checklistItems.map((item) => {
                  const isCritical = item.quantityOnHand <= item.reorderThreshold * 0.5
                  return (
                    <label
                      key={item.id}
                      style={{
                        display: 'flex',
                        gap: '0.75rem',
                        alignItems: 'flex-start',
                        padding: '0.5rem 0.6rem',
                        borderRadius: '10px',
                        border: '1px solid rgba(148, 163, 184, 0.2)'
                      }}
                    >
                      <input
                        type="checkbox"
                        checked={countedItems[item.id] ?? false}
                        onChange={() => toggleChecklistItem(item.id)}
                        style={{ marginTop: '0.35rem' }}
                      />
                      <div style={{ flex: 1 }}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                          <strong>{item.product.name}</strong>
                          <span style={{ fontSize: '0.8rem', color: isCritical ? '#ef4444' : '#f59e0b' }}>
                            {isCritical ? 'Critical' : 'Low'}
                          </span>
                        </div>
                        <small style={{ color: '#94a3b8' }}>
                          {item.product.sku} • {item.quantityOnHand}/{item.reorderThreshold} {item.product.unit}
                        </small>
                      </div>
                    </label>
                  )
                })}
              </div>
            )}
            <div style={{ display: 'flex', gap: '0.5rem' }}>
              <button
                className="btn btn-sm btn-primary"
                onClick={markAllCounted}
                disabled={lowStockItems.length === 0 || countedTotal === lowStockItems.length}
              >
                Mark all counted
              </button>
              <button
                className="btn btn-sm"
                onClick={resetChecklist}
                disabled={countedTotal === 0}
              >
                Reset
              </button>
            </div>
          </div>

          <div className="card" style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <h3 style={{ margin: 0 }}>Smart Reorder Queue</h3>
              <span style={{ fontSize: '0.8rem', color: '#6366f1' }}>{coverageScore}% overall coverage</span>
            </div>
            {reorderTop.length === 0 ? (
              <p style={{ margin: 0, color: '#94a3b8' }}>All items are above their reorder points.</p>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                {reorderTop.map((item) => (
                  <div
                    key={item.id}
                    style={{
                      display: 'grid',
                      gridTemplateColumns: '1fr auto',
                      gap: '0.75rem',
                      padding: '0.75rem',
                      borderRadius: '12px',
                      border: '1px solid rgba(148, 163, 184, 0.2)',
                      background: item.severity === 'critical' ? 'rgba(239, 68, 68, 0.05)' : 'rgba(245, 158, 11, 0.05)'
                    }}
                  >
                    <div>
                      <strong>{item.product}</strong>
                      <div style={{ color: '#94a3b8', fontSize: '0.85rem' }}>{item.sku} • {item.category}</div>
                      <small style={{ color: '#94a3b8' }}>
                        {item.coverageDays === '∞' ? 'Plenty in stock' : `~${item.coverageDays} days of cover`}
                      </small>
                    </div>
                    <div style={{ textAlign: 'right' }}>
                      <span style={{ display: 'block', fontSize: '0.75rem', color: '#94a3b8' }}>Recommended order</span>
                      <div style={{ fontSize: '1.25rem', fontWeight: 600 }}>
                        {item.recommendedOrder} <span style={{ fontSize: '0.85rem' }}>{item.unit}</span>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

          <div className="card" style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
            <h3 style={{ margin: 0 }}>Category Fill Rate</h3>
            {categoryHealth.length === 0 ? (
              <p style={{ margin: 0, color: '#94a3b8' }}>No stock captured for this branch yet.</p>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.7rem' }}>
                {categoryHealth.map((entry) => (
                  <div key={entry.category}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.9rem' }}>
                      <span>{entry.category}</span>
                      <span>{entry.fillRate}%</span>
                    </div>
                    <div style={{ height: 6, borderRadius: 999, background: 'rgba(148, 163, 184, 0.3)', margin: '0.35rem 0' }}>
                      <div
                        style={{
                          width: `${entry.fillRate}%`,
                          height: '100%',
                          borderRadius: 999,
                          background: entry.fillRate >= 90 ? '#22c55e' : entry.fillRate >= 75 ? '#facc15' : '#ef4444'
                        }}
                      />
                    </div>
                    <small style={{ color: '#94a3b8' }}>
                      {entry.lowItems} of {entry.totalItems} items below threshold
                    </small>
                  </div>
                ))}
              </div>
            )}
          </div>
        </section>

      {/* Tabs */}
      <div style={{ display: 'flex', gap: '0.5rem', marginBottom: '1rem' }}>
        <button
          className={`btn ${activeTab === 'stock' ? 'btn-primary' : ''}`}
          onClick={() => setActiveTab('stock')}
        >
          Stock Levels
        </button>
        <button
          className={`btn ${activeTab === 'products' ? 'btn-primary' : ''}`}
          onClick={() => setActiveTab('products')}
        >
          Products
        </button>
        <button
          className="btn"
          onClick={() => setShowAddProduct(true)}
          style={{ marginLeft: 'auto' }}
        >
          + Add Product
        </button>
      </div>

      {/* Add Product Modal */}
      {showAddProduct && (
        <div style={{
          position: 'fixed',
          top: 0, left: 0, right: 0, bottom: 0,
          background: 'rgba(0,0,0,0.7)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          zIndex: 1000
        }}>
          <div className="card" style={{ width: '400px', maxWidth: '90%' }}>
            <h3 style={{ marginTop: 0 }}>Add New Product</h3>
            
            <div className="form-group">
              <label>SKU</label>
              <input
                type="text"
                value={newProduct.sku}
                onChange={e => setNewProduct({ ...newProduct, sku: e.target.value })}
                placeholder="e.g., TOMATO-001"
              />
            </div>
            
            <div className="form-group">
              <label>Name</label>
              <input
                type="text"
                value={newProduct.name}
                onChange={e => setNewProduct({ ...newProduct, name: e.target.value })}
                placeholder="e.g., Fresh Tomatoes"
              />
            </div>
            
            <div className="form-group">
              <label>Category</label>
              <select
                value={newProduct.category}
                onChange={e => setNewProduct({ ...newProduct, category: e.target.value })}
              >
                <option value="">Select category</option>
                <option value="PRODUCE">Produce</option>
                <option value="DAIRY">Dairy</option>
                <option value="MEAT">Meat</option>
                <option value="BAKERY">Bakery</option>
                <option value="BEVERAGES">Beverages</option>
                <option value="SUPPLIES">Supplies</option>
              </select>
            </div>
            
            <div className="form-group">
              <label>Unit</label>
              <input
                type="text"
                value={newProduct.unit}
                onChange={e => setNewProduct({ ...newProduct, unit: e.target.value })}
                placeholder="e.g., kg, units, liters"
              />
            </div>
            
            <div style={{ display: 'flex', gap: '0.5rem', marginTop: '1rem' }}>
              <button
                className="btn btn-primary"
                onClick={handleAddProduct}
                disabled={!newProduct.sku || !newProduct.name || !newProduct.category}
              >
                Add Product
              </button>
              <button className="btn" onClick={() => setShowAddProduct(false)}>
                Cancel
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Add Stock Item Modal */}
      {showAddStock && (
        <div style={{
          position: 'fixed',
          top: 0, left: 0, right: 0, bottom: 0,
          background: 'rgba(0,0,0,0.7)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          zIndex: 1000
        }}>
          <div className="card" style={{ width: '400px', maxWidth: '90%' }}>
            <h3 style={{ marginTop: 0 }}>Add Stock Item</h3>
            
            <div className="form-group">
              <label>Product</label>
              <select
                value={newStock.productId || ''}
                onChange={e => setNewStock({ ...newStock, productId: Number(e.target.value) })}
              >
                <option value="">Select a product</option>
                {productsWithoutStock.map(p => (
                  <option key={p.id} value={p.id}>{p.name} ({p.sku})</option>
                ))}
              </select>
            </div>
            
            <div className="form-group">
              <label>Current Quantity</label>
              <input
                type="number"
                value={newStock.quantityOnHand}
                onChange={e => setNewStock({ ...newStock, quantityOnHand: Number(e.target.value) })}
                min="0"
              />
            </div>
            
            <div className="form-group">
              <label>Reorder Threshold</label>
              <input
                type="number"
                value={newStock.reorderThreshold}
                onChange={e => setNewStock({ ...newStock, reorderThreshold: Number(e.target.value) })}
                min="1"
              />
              <small style={{ color: '#888' }}>Alert when quantity falls below this level</small>
            </div>
            
            <div style={{ display: 'flex', gap: '0.5rem', marginTop: '1rem' }}>
              <button
                className="btn btn-primary"
                onClick={handleAddStockItem}
                disabled={!newStock.productId}
              >
                Add to Inventory
              </button>
              <button className="btn" onClick={() => setShowAddStock(false)}>
                Cancel
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Stock Tab */}
      {activeTab === 'stock' && (
        <div className="card">
          {productsWithoutStock.length > 0 && (
            <div style={{ 
              marginBottom: '1rem', 
              padding: '0.75rem',
              background: 'rgba(59, 130, 246, 0.1)',
              borderRadius: '8px',
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center'
            }}>
              <span style={{ fontSize: '0.9rem' }}>
                {productsWithoutStock.length} product(s) not yet tracked in this branch
              </span>
              <button 
                className="btn btn-sm"
                onClick={() => setShowAddStock(true)}
              >
                + Add Stock Item
              </button>
            </div>
          )}
          
          {stock.length === 0 ? (
            <p style={{ textAlign: 'center', color: '#888' }}>
              No stock items for this branch yet.
            </p>
          ) : (
            <table className="data-table" style={{ width: '100%' }}>
              <thead>
                <tr>
                  <th>Product</th>
                  <th>SKU</th>
                  <th>Category</th>
                  <th>Quantity</th>
                  <th>Threshold</th>
                  <th>Status</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {stock.map(item => {
                  const isEditing = editingStockId === item.id
                  const isCritical = item.quantityOnHand <= item.reorderThreshold * 0.5
                  const isWarning = !isCritical && item.quantityOnHand < item.reorderThreshold
                  
                  return (
                    <tr key={item.id} style={{
                      background: isCritical ? 'rgba(239, 68, 68, 0.05)' : isWarning ? 'rgba(245, 158, 11, 0.05)' : undefined
                    }}>
                      <td><strong>{item.product.name}</strong></td>
                      <td style={{ color: '#888' }}>{item.product.sku}</td>
                      <td>{item.product.category}</td>
                      <td>
                        {isEditing ? (
                          <input
                            type="number"
                            value={editValues.quantity}
                            onChange={e => setEditValues({ ...editValues, quantity: Number(e.target.value) })}
                            style={{ width: '80px' }}
                            min="0"
                          />
                        ) : (
                          <>
                            <span style={{ 
                              fontWeight: 600,
                              color: isCritical ? '#ef4444' : isWarning ? '#f59e0b' : '#4caf50'
                            }}>
                              {item.quantityOnHand}
                            </span>
                            {' '}{item.product.unit}
                          </>
                        )}
                      </td>
                      <td>
                        {isEditing ? (
                          <input
                            type="number"
                            value={editValues.threshold}
                            onChange={e => setEditValues({ ...editValues, threshold: Number(e.target.value) })}
                            style={{ width: '80px' }}
                            min="1"
                          />
                        ) : (
                          <>{item.reorderThreshold} {item.product.unit}</>
                        )}
                      </td>
                      <td>
                        {isCritical ? (
                          <span style={{
                            padding: '0.2rem 0.5rem',
                            borderRadius: '4px',
                            background: 'rgba(239, 68, 68, 0.15)',
                            color: '#ef4444',
                            fontSize: '0.85rem'
                          }}>
                            🚨 Critical
                          </span>
                        ) : isWarning ? (
                          <span style={{
                            padding: '0.2rem 0.5rem',
                            borderRadius: '4px',
                            background: 'rgba(245, 158, 11, 0.15)',
                            color: '#f59e0b',
                            fontSize: '0.85rem'
                          }}>
                            ⚠️ Low
                          </span>
                        ) : (
                          <span style={{
                            padding: '0.2rem 0.5rem',
                            borderRadius: '4px',
                            background: 'rgba(76, 175, 80, 0.15)',
                            color: '#4caf50',
                            fontSize: '0.85rem'
                          }}>
                            ✓ OK
                          </span>
                        )}
                      </td>
                      <td>
                        {isEditing ? (
                          <div style={{ display: 'flex', gap: '0.25rem' }}>
                            <button
                              className="btn btn-sm btn-primary"
                              onClick={() => handleSaveEdit(item.id)}
                              disabled={saving}
                            >
                              {saving ? '...' : '✓'}
                            </button>
                            <button
                              className="btn btn-sm"
                              onClick={handleCancelEdit}
                              disabled={saving}
                            >
                              ✗
                            </button>
                          </div>
                        ) : (
                          <button
                            className="btn btn-sm"
                            onClick={() => handleStartEdit(item)}
                          >
                            Edit
                          </button>
                        )}
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          )}
        </div>
      )}

      {/* Products Tab */}
      {activeTab === 'products' && (
        <div className="card">
          {products.length === 0 ? (
            <p style={{ textAlign: 'center', color: '#888' }}>
              No products configured yet. Add your first product!
            </p>
          ) : (
            <table className="data-table" style={{ width: '100%' }}>
              <thead>
                <tr>
                  <th>Name</th>
                  <th>SKU</th>
                  <th>Category</th>
                  <th>Unit</th>
                </tr>
              </thead>
              <tbody>
                {products.map(product => (
                  <tr key={product.id}>
                    <td><strong>{product.name}</strong></td>
                    <td style={{ color: '#888' }}>{product.sku}</td>
                    <td>
                      <span style={{
                        padding: '0.2rem 0.5rem',
                        borderRadius: '4px',
                        background: 'rgba(96, 165, 250, 0.15)',
                        color: '#60a5fa',
                        fontSize: '0.85rem'
                      }}>
                        {product.category}
                      </span>
                    </td>
                    <td>{product.unit}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      )}
    </div>
  )
}

export default InventoryPage
