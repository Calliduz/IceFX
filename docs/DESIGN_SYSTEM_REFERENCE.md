# IceFX Design System Quick Reference

## Color Palette

### Primary Colors

```
Primary Blue:    #1976D2  (Buttons, headers, links)
Primary Hover:   #1565C0  (Button hover state)
Primary Pressed: #0D47A1  (Button pressed state)
Primary Light:   #BBDEFB  (Selection backgrounds)
```

### Semantic Colors

```
Success Green:   #4CAF50  (Success messages, active status)
Success Hover:   #388E3C  (Success button hover)
Warning Orange:  #FF9800  (Warnings, pending states)
Warning Hover:   #F57C00  (Warning button hover)
Error Red:       #F44336  (Errors, destructive actions)
Error Hover:     #D32F2F  (Error button hover)
Info Blue:       #2196F3  (Information, neutral actions)
Info Hover:      #1976D2  (Info button hover)
```

### Grayscale

```
Gray 50:   #FAFAFA  (Subtle backgrounds)
Gray 100:  #F5F5F5  (Hover backgrounds)
Gray 200:  #EEEEEE  (Borders, dividers)
Gray 300:  #E0E0E0  (Borders)
Gray 400:  #BDBDBD  (Disabled text)
Gray 500:  #9E9E9E  (Placeholder text)
Gray 600:  #757575  (Secondary text)
Gray 700:  #616161  (Body text)
Gray 800:  #424242  (Dark text)
Gray 900:  #212121  (Primary text)
```

### Surface Colors

```
Background:  #F8F9FA  (App background)
Surface 1°:  #FFFFFF  (Cards, panels)
Surface 2°:  #FAFAFA  (Nested cards)
Surface 3°:  #F5F5F5  (Input backgrounds)
```

## Typography

### Font Sizes

```
xs:    11px  (Captions, small labels)
sm:    13px  (Secondary text, table cells)
base:  14px  (Body text, inputs)
lg:    16px  (Emphasized text)
xl:    18px  (Subheadings)
2xl:   24px  (Section titles)
3xl:   32px  (Page titles, large stats)
```

### Font Weights

```
Regular:  400  (Body text)
Semibold: 600  (Emphasized text, labels)
Bold:     700  (Headings, buttons)
```

### Font Stack

```css
font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto,
  "Helvetica Neue", Arial, sans-serif;
```

## Spacing Scale (8px Grid)

```
xs:   4px   (Tight spacing, icon gaps)
sm:   8px   (Small gaps between related items)
md:   16px  (Standard gaps between components)
lg:   24px  (Large gaps between sections)
xl:   32px  (Extra large gaps)
2xl:  48px  (Major section spacing)
3xl:  64px  (Page-level spacing)
```

## Border Radius

```
sm:    4px    (Small elements, badges)
md:    8px    (Buttons, inputs, cards)
lg:    12px   (Large cards, panels)
xl:    16px   (Hero cards, stat cards)
full:  9999px (Circular elements, pills)
```

## Shadows (Depth Levels)

```css
/* Small - Subtle elevation */
sm: dropshadow(gaussian, rgba(0, 0, 0, 0.08), 4, 0, 0, 2);

/* Medium - Standard cards */
md: dropshadow(gaussian, rgba(0, 0, 0, 0.12), 8, 0, 0, 4);

/* Large - Elevated panels */
lg: dropshadow(gaussian, rgba(0, 0, 0, 0.16), 16, 0, 0, 8);

/* Extra Large - Modals, popovers */
xl: dropshadow(gaussian, rgba(0, 0, 0, 0.2), 24, 0, 0, 12);
```

## CSS Classes

### Layout

```css
.card              /* White card with shadow */
/* White card with shadow */
.card-compact      /* Card with reduced padding */
.card-elevated     /* Card with larger shadow */
.header; /* Application header bar */
```

### Buttons

```css
.button            /* Default button */
/* Default button */
.button-primary    /* Primary action (blue) */
.button-success    /* Success action (green) */
.button-warning    /* Warning action (orange) */
.button-danger     /* Destructive action (red) */
.button-info       /* Info action (light blue) */
.button-sm         /* Small button */
.button-lg         /* Large button */
.icon-button; /* Circular icon button */
```

### Statistics Cards

```css
.stat-card-success  /* Green gradient card */
/* Green gradient card */
.stat-card-info     /* Blue gradient card */
.stat-card-warning  /* Orange gradient card */
.stat-card-error; /* Red gradient card */
```

### Text Utilities

```css
.label             /* Standard label */
/* Standard label */
.label-bold        /* Bold label */
.label-secondary   /* Secondary color text */
.label-sm          /* Small text */
.label-lg; /* Large text */
```

### Badges

```css
.badge             /* Base badge */
/* Base badge */
.badge-success     /* Green badge */
.badge-error       /* Red badge */
.badge-warning     /* Orange badge */
.badge-info; /* Blue badge */
```

### Alignment

```css
.text-center       /* Center aligned */
/* Center aligned */
.text-left         /* Left aligned */
.text-right; /* Right aligned */
```

### Spacing Utilities

```css
.mt-sm, .mt-md, .mt-lg  /* Margin top */
.mb-sm, .mb-md, .mb-lg  /* Margin bottom */
.p-sm, .p-md, .p-lg     /* Padding all sides */
.gap-sm, .gap-md, .gap-lg; /* Gap between children */
```

## Component Guidelines

### Cards

```xml
<VBox styleClass="card">
    <Label text="Card Title" styleClass="card-title"/>
    <Separator/>
    <!-- Card content -->
</VBox>
```

### Headers with User Info

```xml
<HBox styleClass="header">
    <Label text="App Title" styleClass="app-title"/>
    <Region HBox.hgrow="ALWAYS"/>
    <VBox>
        <Label fx:id="userName" styleClass="user-info"/>
        <Label fx:id="userRole" styleClass="user-info"/>
    </VBox>
    <Button text="Logout" styleClass="logout-button"/>
</HBox>
```

### Stat Cards

```xml
<VBox styleClass="stat-card-success">
    <Label text="LABEL" styleClass="stat-label"/>
    <Label text="123" styleClass="stat-value"/>
    <Label text="Description" styleClass="stat-description"/>
</VBox>
```

### Buttons with Icons

```xml
<Button text="Action" styleClass="button-primary">
    <graphic>
        <Label text="✓" style="-fx-font-size: 16px;"/>
    </graphic>
</Button>
```

### Empty States

```xml
<VBox alignment="CENTER" spacing="16">
    <Label text="📋" style="-fx-font-size: 64px; -fx-opacity: 0.3;"/>
    <Label text="No Data" styleClass="label-bold"/>
    <Label text="Helpful message" styleClass="label-secondary"/>
</VBox>
```

## Icon Reference (Emoji)

### Common Icons

```
📹 Camera
📋 List/Table
👥 Users
📝 Edit/Form
🔍 Search
🔄 Refresh
💾 Save
➕ Add
🗑 Delete
✖ Close/Clear
🚪 Logout
⚙ Settings
📊 Statistics
🎓 Training
🤖 AI/Model
✅ Success
❓ Unknown
⚠️ Warning
⏳ Loading
😊 Smile/Ready
●  Status Indicator
```

### Status Icons

```
✅ Recognized
❓ Unknown
⚠️ Low Confidence
⏳ Debounced
🔍 Scanning
💡 Information
```

## Best Practices

### DO ✅

- Use design tokens (variables) for consistency
- Apply proper spacing from the 8px grid
- Use semantic colors for actions (green = success, red = danger)
- Include icons for better visual communication
- Add loading states for async operations
- Use toasts for non-blocking feedback
- Provide empty states with helpful messages
- Ensure proper contrast ratios
- Use large touch targets (min 40px height)
- Group related items in cards

### DON'T ❌

- Hardcode colors or spacing values
- Use blocking Alert dialogs
- Forget loading/empty states
- Mix inline styles with CSS classes
- Use tiny fonts (below 12px)
- Create small buttons (below 32px height)
- Overuse shadows (max 2-3 depth levels)
- Ignore user feedback (always show result)
- Use excessive colors (stick to palette)
- Create inconsistent spacing

## Toast Notifications

### Usage

```java
// Success (green)
ModernToast.success("Operation completed successfully");

// Error (red)
ModernToast.error("Failed to save: " + errorMessage);

// Warning (orange)
ModernToast.warning("Please confirm this action");

// Info (blue)
ModernToast.info("Processing your request...");
```

### When to Use Each Type

- **Success**: Completed actions, data saved, user added
- **Error**: Failed operations, validation errors, exceptions
- **Warning**: Confirmations, cautionary messages, deprecations
- **Info**: Status updates, neutral information, processing

## Accessibility

### Contrast Ratios

```
Primary Text (#212121) on White: 16.1:1 ✅
Secondary Text (#616161) on White: 7.1:1 ✅
Button Text (White) on Primary: 5.7:1 ✅
```

### Focus States

- All interactive elements have visible focus indicators
- Focus color: Primary Blue (#1976D2)
- Focus outline: 2px solid, 4px offset

### Keyboard Navigation

- Tab order follows visual flow
- Enter submits forms
- Escape closes dialogs
- All buttons accessible via keyboard

## Responsive Breakpoints

```
Dashboard:    1600x900px  (Standard layout)
Admin Panel:  1800x950px  (Larger for data-heavy interface)
Minimum:      1280x720px  (Supported but not optimal)
```

---

## Quick Copy-Paste Snippets

### Standard Card

```xml
<VBox styleClass="card" spacing="16">
    <HBox alignment="CENTER_LEFT" spacing="12">
        <Label text="🔧" style="-fx-font-size: 24px;"/>
        <Label text="Section Title" styleClass="card-title"/>
    </HBox>
    <Separator/>
    <!-- Content here -->
</VBox>
```

### Action Button Group

```xml
<HBox spacing="12" alignment="CENTER">
    <Button text="Save" styleClass="button-primary, button-lg"/>
    <Button text="Cancel" styleClass="button, button-lg"/>
    <Button text="Delete" styleClass="button-danger, button-lg"/>
</HBox>
```

### Table with Empty State

```xml
<TableView fx:id="myTable">
    <columns>
        <TableColumn text="COLUMN" prefWidth="120"/>
    </columns>
    <placeholder>
        <VBox alignment="CENTER" spacing="16">
            <Label text="📋" style="-fx-font-size: 64px; -fx-opacity: 0.3;"/>
            <Label text="No data available" styleClass="label-secondary"/>
        </VBox>
    </placeholder>
</TableView>
```

### Form Row

```xml
<GridPane hgap="16" vgap="16">
    <Label text="Field Name:" styleClass="label-bold"
           GridPane.columnIndex="0" GridPane.rowIndex="0"/>
    <TextField fx:id="field" promptText="Enter value"
               GridPane.columnIndex="1" GridPane.rowIndex="0"/>
</GridPane>
```

---

**Version**: 3.0  
**Last Updated**: January 2025  
**Maintainer**: IceFX Team
