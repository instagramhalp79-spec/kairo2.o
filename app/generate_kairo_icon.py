import os
import subprocess

svg_content = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512" width="512" height="512">
  <defs>
    <!-- Background Radial Gradients -->
    <radialGradient id="neonGlow" cx="50%" cy="50%" r="50%">
      <stop offset="70%" stop-color="#FF1E27" stop-opacity="0.85" />
      <stop offset="85%" stop-color="#FF0033" stop-opacity="0.4" />
      <stop offset="100%" stop-color="#000000" stop-opacity="0" />
    </radialGradient>
    
    <radialGradient id="innerBadgeGrad" cx="35%" cy="30%" r="65%">
      <stop offset="0%" stop-color="#181920" />
      <stop offset="60%" stop-color="#0C0D11" />
      <stop offset="100%" stop-color="#050608" />
    </radialGradient>

    <!-- Metallic Red 3D Gradients -->
    <linearGradient id="redPillarGrad" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#FF5C68" />
      <stop offset="25%" stop-color="#FF2A38" />
      <stop offset="70%" stop-color="#D60714" />
      <stop offset="100%" stop-color="#800008" />
    </linearGradient>

    <linearGradient id="redArmTopGrad" x1="0%" y1="100%" x2="100%" y2="0%">
      <stop offset="0%" stop-color="#D60714" />
      <stop offset="40%" stop-color="#FF2A38" />
      <stop offset="80%" stop-color="#FF5E6A" />
      <stop offset="100%" stop-color="#FFA4AC" />
    </linearGradient>

    <linearGradient id="redArmBottomGrad" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#D60714" />
      <stop offset="50%" stop-color="#FF2A38" />
      <stop offset="85%" stop-color="#FF4B5A" />
      <stop offset="100%" stop-color="#800008" />
    </linearGradient>

    <linearGradient id="redNoteGrad" x1="20%" y1="0%" x2="80%" y2="100%">
      <stop offset="0%" stop-color="#FF6B76" />
      <stop offset="40%" stop-color="#FF2030" />
      <stop offset="85%" stop-color="#C40511" />
      <stop offset="100%" stop-color="#730006" />
    </linearGradient>

    <linearGradient id="ringEdgeGrad" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#FFAEB4" />
      <stop offset="20%" stop-color="#FF2434" />
      <stop offset="60%" stop-color="#B8000A" />
      <stop offset="100%" stop-color="#FF1A2A" />
    </linearGradient>

    <filter id="neonBlur" x="-20%" y="-20%" width="140%" height="140%">
      <feGaussianBlur stdDeviation="8" result="blur" />
      <feComposite in="SourceGraphic" in2="blur" operator="over" />
    </filter>

    <filter id="dropShadow" x="-20%" y="-20%" width="140%" height="140%">
      <feDropShadow dx="0" dy="6" stdDeviation="6" flood-color="#000000" flood-opacity="0.8" />
    </filter>
  </defs>

  <!-- Solid Black Canvas -->
  <rect width="512" height="512" fill="#000000" />

  <!-- Outer Neon Glow Halos -->
  <circle cx="256" cy="256" r="236" fill="none" stroke="#FF002B" stroke-width="12" opacity="0.35" filter="url(#neonBlur)" />
  <circle cx="256" cy="256" r="232" fill="none" stroke="#FF2B38" stroke-width="7" opacity="0.65" filter="url(#neonBlur)" />

  <!-- Inner Badge Plate -->
  <circle cx="256" cy="256" r="226" fill="url(#innerBadgeGrad)" stroke="url(#ringEdgeGrad)" stroke-width="4.5" />

  <!-- Inner Recessed Subtle Rim -->
  <circle cx="256" cy="256" r="221" fill="none" stroke="#000000" stroke-width="3" opacity="0.9" />

  <!-- MAIN EMBLEM (K + Musical Note) with Drop Shadow -->
  <g filter="url(#dropShadow)">
    
    <!-- 1. Left Vertical Stem of 'K' -->
    <path d="M 144 88 
             C 160 86, 194 92, 194 116 
             L 194 274 
             C 194 294, 160 300, 144 298 
             C 144 298, 144 88, 144 88 Z" 
          fill="url(#redPillarGrad)" />
    <!-- Glossy Highlight curve on left stem -->
    <path d="M 144 88 
             C 172 108, 192 165, 144 298 
             L 144 88 Z" 
          fill="#FF7580" opacity="0.45" />

    <!-- 2. Upper Diagonal Arm of 'K' -->
    <path d="M 172 208 
             L 278 102 
             C 290 90, 310 90, 354 90 
             L 354 108 
             L 242 220 
             Z" 
          fill="url(#redArmTopGrad)" />

    <!-- 3. Lower Diagonal Leg of 'K' -->
    <path d="M 218 196 
             L 316 294 
             C 330 300, 342 300, 342 290 
             L 262 198 
             Z" 
          fill="url(#redArmBottomGrad)" />
    <!-- Lower leg extension / thickness -->
    <path d="M 200 188 
             L 290 286 
             C 308 300, 334 300, 342 290 
             L 254 200 
             Z" 
          fill="url(#redArmBottomGrad)" />

    <!-- 4. 3D Glossy Musical Note (Connected & Nested at upper right) -->
    <g transform="translate(10, 10)">
      <!-- Left Note Head -->
      <ellipse cx="320" cy="226" rx="21" ry="15" transform="rotate(-25 320 226)" fill="url(#redNoteGrad)" />
      <!-- Right Note Head -->
      <ellipse cx="370" cy="242" rx="21" ry="15" transform="rotate(-25 370 242)" fill="url(#redNoteGrad)" />

      <!-- Left Vertical Stem -->
      <rect x="330" y="152" width="9.5" height="74" rx="4" fill="url(#redNoteGrad)" />
      <!-- Right Vertical Stem -->
      <rect x="380" y="168" width="9.5" height="74" rx="4" fill="url(#redNoteGrad)" />

      <!-- Top Connecting Beamed Bar -->
      <path d="M 330 152 
               L 390 168 
               L 390 186 
               L 330 170 Z" 
            fill="url(#redNoteGrad)" />
      <!-- Top Beam Highlight -->
      <path d="M 330 152 
               L 390 168 
               L 390 174 
               L 330 158 Z" 
            fill="#FFA6AE" opacity="0.6" />
    </g>
  </g>

  <!-- KAIRO Typography (Geometric Pure White with uncrossed Lambda 'A') -->
  <g fill="#FFFFFF" filter="url(#dropShadow)">
    <!-- Letter 'K' -->
    <path d="M 104 330 L 120 330 L 120 380 L 104 380 Z" />
    <path d="M 148 330 L 122 355 L 148 380 L 130 380 L 112 360 L 112 350 L 128 330 Z" />

    <!-- Letter 'Λ' (Stylized Lambda / Uncrossed A) -->
    <path d="M 184 330 L 202 380 L 186 380 L 175 348 L 164 380 L 148 380 L 166 330 Z" />

    <!-- Letter 'I' -->
    <rect x="220" y="330" width="16" height="50" rx="1" />

    <!-- Letter 'R' -->
    <path d="M 252 330 L 282 330 C 295 330, 303 336, 303 348 C 303 358, 296 364, 284 366 L 305 380 L 286 380 L 268 366 L 268 380 L 252 380 Z
             M 268 344 L 268 354 L 280 354 C 285 354, 288 352, 288 349 C 288 346, 285 344, 280 344 Z" />

    <!-- Letter 'O' -->
    <path d="M 340 330 
             C 358 330, 370 341, 370 355 
             C 370 369, 358 380, 340 380 
             C 322 380, 310 369, 310 355 
             C 310 341, 322 330, 340 330 Z
             M 340 343 
             C 331 343, 326 348, 326 355 
             C 326 362, 331 367, 340 367 
             C 349 367, 354 362, 354 355 
             C 354 348, 349 343, 340 343 Z" />
  </g>
</svg>
"""

with open("kairo_icon.svg", "w") as f:
    f.write(svg_content)

print("SVG written to kairo_icon.svg")
