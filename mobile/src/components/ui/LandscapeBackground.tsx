
import { StyleSheet } from 'react-native';
import Svg, {
  Circle,
  Defs,
  LinearGradient,
  Path,
  Stop,
} from 'react-native-svg';

export function LandscapeBackground() {
  return (
    <Svg
      style={StyleSheet.absoluteFill}
      width="100%"
      height="100%"
      viewBox="0 0 390 910"
      preserveAspectRatio="xMidYMid slice"
      pointerEvents="none"
      accessibilityElementsHidden
      importantForAccessibility="no-hide-descendants"
    >
      <Defs>
        {/* Warm ivory background */}
        <LinearGradient id="ivory" x1="0" y1="0" x2="1" y2="1">
          <Stop offset="0" stopColor="#F5F5EC" />
          <Stop offset="0.55" stopColor="#FBFAF4" />
          <Stop offset="1" stopColor="#F1F2E7" />
        </LinearGradient>

        {/* Botanical leaf */}
        <LinearGradient id="darkLeaf" x1="0" y1="0" x2="1" y2="1">
          <Stop offset="0" stopColor="#39745A" />
          <Stop offset="1" stopColor="#174C3A" />
        </LinearGradient>

        {/* Plant leaves */}
        <LinearGradient id="plantLeaf" x1="0" y1="0" x2="1" y2="1">
          <Stop offset="0" stopColor="#A5C69C" />
          <Stop offset="1" stopColor="#39775C" />
        </LinearGradient>

        {/* Distant pale hills */}
        <LinearGradient id="hill1" x1="0" y1="0" x2="0" y2="1">
          <Stop offset="0" stopColor="#E0EAD9" />
          <Stop offset="1" stopColor="#C8DCC7" />
        </LinearGradient>

        {/* Middle hills */}
        <LinearGradient id="hill2" x1="0" y1="0" x2="0.8" y2="1">
          <Stop offset="0" stopColor="#A9CBAE" />
          <Stop offset="1" stopColor="#80B197" />
        </LinearGradient>

        <LinearGradient id="hill3" x1="0" y1="0" x2="1" y2="1">
          <Stop offset="0" stopColor="#76A98C" />
          <Stop offset="1" stopColor="#4F8B72" />
        </LinearGradient>

        <LinearGradient id="hill4" x1="0" y1="0" x2="1" y2="1">
          <Stop offset="0" stopColor="#43866C" />
          <Stop offset="1" stopColor="#28684F" />
        </LinearGradient>

        {/* Cream transition: gradually blends over the hills */}
        <LinearGradient id="creamFade" x1="0" y1="0" x2="0" y2="1">
          <Stop offset="0" stopColor="#F8F7F2" stopOpacity={0} />
          <Stop offset="0.15" stopColor="#F8F7F2" stopOpacity={0.35} />
          <Stop offset="0.35" stopColor="#F8F7F2" stopOpacity={0.78} />
          <Stop offset="0.65" stopColor="#F8F7F2" stopOpacity={0.96} />
          <Stop offset="0.85" stopColor="#F8F7F2" stopOpacity={0.88} />
          <Stop offset="1" stopColor="#F8F7F2" stopOpacity={0.2} />
        </LinearGradient>

        {/* Sage green transition */}
        <LinearGradient id="sageFade" x1="0" y1="0" x2="0" y2="1">
          <Stop offset="0" stopColor="#B9D1B7" stopOpacity={0} />
          <Stop offset="0.2" stopColor="#B9D1B7" stopOpacity={0.3} />
          <Stop offset="0.5" stopColor="#B9D1B7" stopOpacity={0.75} />
          <Stop offset="1" stopColor="#B9D1B7" stopOpacity={0.95} />
        </LinearGradient>

        {/* Forest green transition */}
        <LinearGradient id="forestFade" x1="0" y1="0" x2="0" y2="1">
          <Stop offset="0" stopColor="#28694F" stopOpacity={0} />
          <Stop offset="0.3" stopColor="#28694F" stopOpacity={0.35} />
          <Stop offset="0.65" stopColor="#28694F" stopOpacity={0.75} />
          <Stop offset="1" stopColor="#174B39" stopOpacity={0.95} />
        </LinearGradient>

        {/* Deepest bottom green */}
        <LinearGradient id="bottomHill" x1="0" y1="0" x2="0" y2="1">
          <Stop offset="0" stopColor="#39785D" />
          <Stop offset="1" stopColor="#174B39" />
        </LinearGradient>
      </Defs>

      {/* ================================= */}
      {/* BASE BACKGROUND                   */}
      {/* ================================= */}

      <Path d="M0 0 H390 V910 H0 Z" fill="url(#ivory)" />

      {/* ================================= */}
      {/* TOP LEFT BOTANICAL LEAVES         */}
      {/* ================================= */}

      <Path
        d="M0 0 L142 0 C128 32 104 54 75 72 C48 89 20 99 0 118 Z"
        fill="#DCE8D7"
      />

      <Path
        d="M0 0 L142 0 C126 53 96 89 60 111 C38 126 16 143 0 164 Z"
        fill="#C8DCC5"
        opacity={0.72}
      />

      <Path
        d="M0 94 C29 62 72 41 101 40 C93 91 60 129 0 165 Z"
        fill="url(#darkLeaf)"
      />

      <Path
        d="M0 163 C29 144 60 133 94 136 C69 158 35 181 0 194 Z"
        fill="#BFD6B9"
        opacity={0.95}
      />

      {/* ================================= */}
      {/* SUN                               */}
      {/* ================================= */}

      <Circle
        cx={315}
        cy={531}
        r={22}
        fill="#F2C98D"
        opacity={0.48}
      />

      {/* ================================= */}
      {/* LAYERED LANDSCAPE                 */}
      {/* ================================= */}

      {/* Furthest pale hill */}
      <Path
        d="
          M0 515
          C42 510 74 514 112 536
          C157 563 200 585 247 574
          C298 562 337 538 390 527
          L390 910 L0 910 Z
        "
        fill="url(#hill1)"
      />

      {/* Second hill */}
      <Path
        d="
          M0 555
          C40 553 75 560 115 578
          C159 598 202 608 248 590
          C297 571 340 555 390 550
          L390 910 L0 910 Z
        "
        fill="#C1D9BD"
        opacity={0.92}
      />

      {/* Third hill */}
      <Path
        d="
          M0 582
          C47 565 88 562 129 579
          C174 598 211 614 257 601
          C309 585 349 568 390 574
          L390 910 L0 910 Z
        "
        fill="url(#hill2)"
      />

      {/* Fourth hill */}
      <Path
        d="
          M0 612
          C43 590 85 589 124 606
          C174 628 208 641 260 619
          C312 597 351 577 390 590
          L390 910 L0 910 Z
        "
        fill="url(#hill3)"
      />

      {/* Foreground hill */}
      <Path
        d="
          M0 632
          C48 621 86 626 127 645
          C177 668 219 674 268 649
          C315 625 354 611 390 617
          L390 910 L0 910 Z
        "
        fill="url(#hill4)"
      />

      {/* ================================= */}
      {/* CREAM AUTHENTICATION AREA         */}
      {/* ================================= */}

      <Path
        d="
          M0 650
          C65 665 110 690 165 685
          C230 680 290 654 390 640
          L390 865
          C315 850 250 855 190 868
          C120 883 55 870 0 850
          Z
        "
        fill="url(#creamFade)"
      />

      {/* ================================= */}
      {/* SOFT SAGE BOTTOM WAVE             */}
      {/* ================================= */}

      <Path
        d="
          M0 850
          C75 875 130 887 195 875
          C265 862 325 835 390 820
          L390 910 L0 910 Z
        "
        fill="url(#sageFade)"
      />

      {/* ================================= */}
      {/* FOREST GREEN BOTTOM WAVE          */}
      {/* ================================= */}

      <Path
        d="
          M0 885
          C80 900 145 904 215 891
          C285 878 345 852 390 842
          L390 910 L0 910 Z
        "
        fill="url(#forestFade)"
      />

      {/* Deep bottom landscape */}
      <Path
        d="
          M0 857
          C54 870 105 882 161 877
          C228 871 290 850 342 838
          C362 833 378 827 390 821
          L390 910 L0 910 Z
        "
        fill="url(#bottomHill)"
        opacity={0.55}
      />

      {/* ================================= */}
      {/* BOTTOM LEFT BOTANICAL PLANT       */}
      {/* ================================= */}

      {/* Main stem */}
      <Path
        d="
          M17 666
          C20 634 18 602 21 571
          C23 549 26 526 30 510
        "
        stroke="#39775C"
        strokeWidth={3}
        strokeLinecap="round"
        fill="none"
      />

      {/* Large upright leaf */}
      <Path
        d="
          M18 582
          C3 561 4 530 17 504
          C39 524 43 555 26 584 Z
        "
        fill="url(#plantLeaf)"
      />

      {/* Upper right leaf */}
      <Path
        d="
          M24 605
          C33 577 59 558 83 558
          C79 584 53 606 24 615 Z
        "
        fill="#6F9F7D"
      />

      {/* Middle left leaf */}
      <Path
        d="
          M18 637
          C-2 620 -11 594 -4 571
          C20 585 33 611 23 639 Z
        "
        fill="#79A98A"
      />

      {/* Middle right leaf */}
      <Path
        d="
          M20 658
          C32 632 58 619 81 623
          C71 648 47 663 20 667 Z
        "
        fill="#39785D"
      />

      {/* Lower left leaf */}
      <Path
        d="
          M16 686
          C-3 673 -13 650 -8 629
          C14 640 28 662 22 688 Z
        "
        fill="#43866C"
      />

      {/* Lower right leaf */}
      <Path
        d="
          M18 699
          C35 674 59 667 81 674
          C68 698 44 712 18 710 Z
        "
        fill="#28684F"
      />

      {/* Bottom left leaf */}
      <Path
        d="
          M15 723
          C-2 708 -10 689 -5 671
          C16 681 28 701 22 724 Z
        "
        fill="#28684F"
      />
    </Svg>
  );
}
 