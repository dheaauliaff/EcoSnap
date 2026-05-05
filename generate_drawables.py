import os

colors = {
    "organik": ("#1A4CAF50", "#3381C784"),
    "kardus": ("#1A2196F3", "#3364B5F6"),
    "kaca": ("#1A00BCD4", "#334DD0E1"),
    "logam": ("#1A9C27B0", "#33BA68C8"),
    "kertas": ("#1AFFC107", "#33FFD54F"),
    "plastik": ("#1AFF9800", "#33FFB74D")
}

path = r"d:\EcoSnap\app\src\main\res\drawable"
os.makedirs(path, exist_ok=True)

for name, (start, end) in colors.items():
    content = f"""<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="oval">
    <gradient
        android:angle="135"
        android:startColor="{start}"
        android:endColor="{end}"
        android:type="linear" />
    <stroke android:width="0.5dp" android:color="#40FFFFFF"/>
</shape>
"""
    with open(os.path.join(path, f"bg_icon_gradient_{name}.xml"), "w") as f:
        f.write(content)

animator_path = r"d:\EcoSnap\app\src\main\res\animator"
os.makedirs(animator_path, exist_ok=True)

animator_content = """<?xml version="1.0" encoding="utf-8"?>
<selector xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:state_pressed="true">
        <set>
            <objectAnimator android:propertyName="scaleX"
                android:duration="150"
                android:valueTo="0.97"
                android:valueType="floatType"/>
            <objectAnimator android:propertyName="scaleY"
                android:duration="150"
                android:valueTo="0.97"
                android:valueType="floatType"/>
            <objectAnimator android:propertyName="translationZ"
                android:duration="150"
                android:valueTo="1dp"
                android:valueType="floatType"/>
        </set>
    </item>
    <item>
        <set>
            <objectAnimator android:propertyName="scaleX"
                android:duration="250"
                android:valueTo="1.0"
                android:valueType="floatType"/>
            <objectAnimator android:propertyName="scaleY"
                android:duration="250"
                android:valueTo="1.0"
                android:valueType="floatType"/>
            <objectAnimator android:propertyName="translationZ"
                android:duration="250"
                android:valueTo="3dp"
                android:valueType="floatType"/>
        </set>
    </item>
</selector>
"""
with open(os.path.join(animator_path, "card_bounce_anim.xml"), "w") as f:
    f.write(animator_content)

bg_card_content = """<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <item>
        <shape android:shape="rectangle">
            <gradient
                android:angle="270"
                android:startColor="#FFFFFF"
                android:endColor="#FAFAFA"
                android:type="linear" />
            <corners android:radius="20dp" />
            <stroke android:width="1dp" android:color="#66EEEEEE" />
        </shape>
    </item>
    <!-- Top inner highlight -->
    <item android:bottom="2dp">
        <shape android:shape="rectangle">
            <stroke android:width="1dp" android:color="#4DFFFFFF" />
            <corners android:radius="20dp" />
        </shape>
    </item>
</layer-list>
"""
with open(os.path.join(path, "bg_premium_card.xml"), "w") as f:
    f.write(bg_card_content)

print("Generation complete.")
