import re

with open(r"d:\EcoSnap\app\src\main\res\layout\activity_dashboard_user.xml", "r", encoding="utf-8") as f:
    content = f.read()

categories = [
    ("Organik", "ic_leaf_outline", "#4CAF50", "5", "marginEnd=\"6dp\"", "organik"),
    ("Kardus", "ic_box_outline", "#2196F3", "10", "marginStart=\"6dp\"", "kardus"),
    ("Kaca", "ic_bottle_outline", "#00BCD4", "7", "marginEnd=\"6dp\"", "kaca"),
    ("Logam", "ic_can_outline", "#9C27B0", "3", "marginStart=\"6dp\"", "logam"),
    ("Kertas", "ic_document_outline", "#FFC107", "8", "marginEnd=\"6dp\"", "kertas"),
    ("Plastik", "ic_plastic_bottle_outline", "#FF9800", "12", "marginStart=\"6dp\"", "plastik"),
]

def build_card(name, icon, color, val, margin, grad_name):
    return f"""                    <!-- {name} -->
                    <LinearLayout
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:layout_weight="1"
                        android:{margin}
                        android:background="@drawable/bg_premium_card"
                        android:elevation="4dp"
                        android:outlineAmbientShadowColor="#0F000000"
                        android:outlineSpotShadowColor="#0A000000"
                        android:stateListAnimator="@animator/card_bounce_anim"
                        android:clickable="true"
                        android:focusable="true"
                        android:orientation="horizontal"
                        android:padding="16dp"
                        android:gravity="center_vertical">
                        
                        <FrameLayout
                            android:layout_width="48dp"
                            android:layout_height="48dp"
                            android:background="@drawable/bg_icon_gradient_{grad_name}">
                            <ImageView
                                android:layout_width="24dp"
                                android:layout_height="24dp"
                                android:layout_gravity="center"
                                android:src="@drawable/{icon}"
                                app:tint="{color}" />
                        </FrameLayout>

                        <LinearLayout
                            android:layout_width="match_parent"
                            android:layout_height="wrap_content"
                            android:layout_marginStart="12dp"
                            android:orientation="vertical"
                            android:gravity="center">
                            <TextView
                                android:layout_width="wrap_content"
                                android:layout_height="wrap_content"
                                android:text="{name}"
                                android:textColor="#212121"
                                android:textSize="14sp" />
                            <TextView
                                android:layout_width="wrap_content"
                                android:layout_height="wrap_content"
                                android:layout_marginTop="4dp"
                                android:text="{val}"
                                android:textColor="{color}"
                                android:textSize="22sp"
                                android:textStyle="bold"
                                android:letterSpacing="0.05" />
                            <TextView
                                android:layout_width="wrap_content"
                                android:layout_height="wrap_content"
                                android:text="items"
                                android:textColor="#757575"
                                android:textSize="12sp" />
                        </LinearLayout>
                    </LinearLayout>"""

row1 = f"""                <!-- ROW 1 -->
                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="horizontal"
                    android:baselineAligned="false"
                    android:clipChildren="false"
                    android:clipToPadding="false">

{build_card(*categories[0])}

{build_card(*categories[1])}
                </LinearLayout>"""

row2 = f"""                <!-- ROW 2 -->
                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="12dp"
                    android:orientation="horizontal"
                    android:baselineAligned="false"
                    android:clipChildren="false"
                    android:clipToPadding="false">

{build_card(*categories[2])}

{build_card(*categories[3])}
                </LinearLayout>"""

row3 = f"""                <!-- ROW 3 -->
                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="12dp"
                    android:orientation="horizontal"
                    android:baselineAligned="false"
                    android:clipChildren="false"
                    android:clipToPadding="false">

{build_card(*categories[4])}

{build_card(*categories[5])}
                </LinearLayout>"""

content = re.sub(r"<!-- ROW 1 -->.*?<!-- ROW 2 -->", row1 + "\n\n                <!-- ROW 2 -->", content, flags=re.DOTALL)
content = re.sub(r"<!-- ROW 2 -->.*?<!-- ROW 3 -->", row2 + "\n\n                <!-- ROW 3 -->", content, flags=re.DOTALL)
content = re.sub(r"<!-- ROW 3 -->.*?(?=</LinearLayout>\n\n            <!-- 5️⃣)", row3 + "\n                ", content, flags=re.DOTALL)

with open(r"d:\EcoSnap\app\src\main\res\layout\activity_dashboard_user.xml", "w", encoding="utf-8") as f:
    f.write(content)

print("Replacement complete.")
