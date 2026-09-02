package com.example.pinmind.ui.theme

import androidx.compose.ui.graphics.Color

// ==========================================
// PinMind Brand Palette (Red, Cream, Ink)
// ==========================================
val PinRed = Color(0xFFE8503A)         // Primary Brand Red: Pins, CTAs, Triggers
val PinRedDeep = Color(0xFFC63A26)     // Deep Pressed / Secondary Red
val Ink = Color(0xFF201912)            // Deep Ink Charcoal: Typography & Dark Surface
val Cream = Color(0xFFFAF4E6)          // Warm Cream: Light Background
val CreamVariant = Color(0xFFF3EBD9)   // Warm Surface Variant: Chips & Borders
val CreamSurface = Color(0xFFFFFDF8)   // Pure Card Surface

// --- Light Theme Colors ---
val PrimaryLight = PinRed
val OnPrimaryLight = Color(0xFFFFFFFF)
val PrimaryContainerLight = Color(0xFFFFDAD4)
val OnPrimaryContainerLight = Color(0xFF410001)

val SecondaryLight = PinRedDeep
val OnSecondaryLight = Color(0xFFFFFFFF)
val SecondaryContainerLight = CreamVariant
val OnSecondaryContainerLight = Ink

val TertiaryLight = Color(0xFF8B5000)
val OnTertiaryLight = Color(0xFFFFFFFF)
val TertiaryContainerLight = Color(0xFFFFDCC0)
val OnTertiaryContainerLight = Color(0xFF2C1600)

val BackgroundLight = Cream
val OnBackgroundLight = Ink
val SurfaceLight = CreamSurface
val OnSurfaceLight = Ink
val SurfaceVariantLight = CreamVariant
val OnSurfaceVariantLight = Color(0xFF56473D)
val OutlineLight = Color(0xFF8B776A)
val OutlineVariantLight = Color(0xFFDCCFBE)

// --- Dark Theme Colors ---
val PrimaryDark = Color(0xFFFF8978)       // Vibrant warm coral red
val OnPrimaryDark = Color(0xFF621006)
val PrimaryContainerDark = Color(0xFF881F12)
val OnPrimaryContainerDark = Color(0xFFFFDAD4)

val SecondaryDark = Color(0xFFE5BDB5)
val OnSecondaryDark = Color(0xFF442823)
val SecondaryContainerDark = Color(0xFF382923)
val OnSecondaryContainerDark = Cream

val TertiaryDark = Color(0xFFFFB777)
val OnTertiaryDark = Color(0xFF4A2800)
val TertiaryContainerDark = Color(0xFF693C00)
val OnTertiaryContainerDark = Color(0xFFFFDCC0)

val BackgroundDark = Color(0xFF16110D)   // Deepest ink black-brown
val OnBackgroundDark = Cream
val SurfaceDark = Ink                    // Brand Ink (#201912)
val OnSurfaceDark = Cream
val SurfaceVariantDark = Color(0xFF322820)
val OnSurfaceVariantDark = Color(0xFFD6C3B7)
val OutlineDark = Color(0xFF9E8C80)
val OutlineVariantDark = Color(0xFF52443B)

// Priority Accent Colors
val PriorityLowColor = Color(0xFF388E3C)
val PriorityMediumColor = Color(0xFFF57C00)
val PriorityHighColor = Color(0xFFE8503A)    // PinRed
val PriorityUrgentColor = Color(0xFF8E24AA)