import os

path = r'd:\EcoSnap\app\src\main\res\drawable'
os.makedirs(path, exist_ok=True)

with open(os.path.join(path, 'bg_green_soft_card.xml'), 'w') as f:
    f.write('''<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <solid android:color="#E8F5E9" />
    <corners android:radius="16dp" />
</shape>''')

with open(os.path.join(path, 'bg_filter_btn.xml'), 'w') as f:
    f.write('''<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <solid android:color="#FFFFFF" />
    <corners android:radius="12dp" />
    <stroke android:width="1dp" android:color="#E0E0E0" />
</shape>''')

with open(os.path.join(path, 'bg_podium_rank1.xml'), 'w') as f:
    f.write('''<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <solid android:color="#2E7D32" />
    <corners android:topLeftRadius="12dp" android:topRightRadius="12dp" />
</shape>''')

with open(os.path.join(path, 'bg_podium_rank2.xml'), 'w') as f:
    f.write('''<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <solid android:color="#1565C0" />
    <corners android:topLeftRadius="12dp" android:topRightRadius="12dp" />
</shape>''')

with open(os.path.join(path, 'bg_podium_rank3.xml'), 'w') as f:
    f.write('''<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <solid android:color="#F57C00" />
    <corners android:topLeftRadius="12dp" android:topRightRadius="12dp" />
</shape>''')

with open(os.path.join(path, 'bg_button_outline.xml'), 'w') as f:
    f.write('''<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <solid android:color="@android:color/transparent" />
    <corners android:radius="24dp" />
    <stroke android:width="1dp" android:color="#4CAF50" />
</shape>''')

print('Drawables generated.')
