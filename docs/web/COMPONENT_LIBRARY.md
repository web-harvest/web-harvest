# WebHarvest Component Library

**Reusable components for Website + IDE**

---

## 🎨 Design Tokens

### Colors
```css
--purple-600: #7C3AED
--pink-600: #DB2777
--amber-500: #F59E0B
--gradient-primary: linear-gradient(135deg, #6366F1, #8B5CF6, #D946EF)
```

### Spacing (8pt grid)
```css
--spacing-1: 0.25rem (4px)
--spacing-2: 0.5rem  (8px)
--spacing-4: 1rem    (16px)
--spacing-6: 1.5rem  (24px)
--spacing-8: 2rem    (32px)
```

---

## 📦 Components

### 1. Header (Navbar)

**Usage:** Website + IDE

```html
<header class="site-header">
  <div class="container header-inner">
    <div class="brand">
      <img src="logo.png" class="logo" />
      <span class="project-name">WebHarvest</span>
    </div>
    <nav class="main-nav">
      <a href="#features">Features</a>
      <a href="#processors">Processors</a>
      <a class="cta" href="#download">Download</a>
    </nav>
  </div>
</header>
```

**Features:**
- Sticky position
- Backdrop blur (macOS style)
- 72px height
- Responsive

---

### 2. Buttons

#### Primary (Yellow CTA)
```html
<a class="btn btn-primary" href="#">
  <i class="fas fa-download"></i>
  Download
</a>
```

#### Secondary (Outline)
```html
<a class="btn btn-secondary" href="#">
  Learn More
</a>
```

#### Large
```html
<a class="btn btn-primary btn-large" href="#">
  Download v2.2.0
</a>
```

**States:**
- Default
- Hover (translateY -2px + shadow)
- Active (translateY 0)
- Focus (yellow outline)

---

### 3. Feature Card

```html
<div class="feature-card">
  <div class="feature-icon">
    <i class="fas fa-code"></i>
  </div>
  <h3>Title</h3>
  <p>Description</p>
  <div class="feature-tags">
    <a href="#" class="tag">&lt;processor&gt;</a>
  </div>
</div>
```

**Features:**
- Gradient icon background
- Shadow on hover
- Border accent on hover
- Lift animation (-4px)

---

### 4. Processor Card

```html
<a href="#" class="processor-card">
  <div class="processor-icon">
    <i class="fas fa-globe"></i>
  </div>
  <h4>HTTP</h4>
  <p>Complete HTTP client</p>
</a>
```

**Features:**
- Clickable (entire card)
- Icon transforms on hover (scale + gradient)
- Auto-grid layout
- Responsive

---

### 5. Code Window (macOS Style)

```html
<div class="code-window">
  <div class="window-header">
    <div class="window-controls">
      <span class="control red"></span>
      <span class="control yellow"></span>
      <span class="control green"></span>
    </div>
    <span class="window-title">file.xml</span>
  </div>
  <div class="window-content">
    <pre><code class="language-xml">...</code></pre>
  </div>
</div>
```

**Usage:** Documentation + IDE preview

---

### 6. Section Header

```html
<div class="section-header">
  <h2>Section Title</h2>
  <p class="section-subtitle">Description text</p>
</div>
```

**Variants:**
- Centered (website)
- Left-aligned (IDE)

---

## 🎯 IDE Adaptation

### Same Components:
- ✅ `.site-header` - navbar
- ✅ `.btn` - all buttons
- ✅ `.code-window` - results display
- ✅ Color variables
- ✅ Spacing system

### IDE-Specific:
- Split panel layout
- Tab bar (extends header pattern)
- Results tabs (same as processor-card pattern)
- Session info cards (extends feature-card pattern)

---

## 📱 Responsive Breakpoints

```
Mobile:  < 640px
Tablet:  640px - 1024px
Desktop: > 1024px
```

**Behavior:**
- Mobile: Stack everything, hide nav (hamburger menu)
- Tablet: 2-column grids
- Desktop: Full multi-column layout

---

## ♿ Accessibility

- Focus states (yellow outline)
- Reduced motion support
- High contrast support
- Semantic HTML
- ARIA labels (where needed)

---

**All components tested and production-ready**


