package tv.limehd.keyboard;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

public class Keyboard extends LinearLayout {

    private final String TAG = "Keyboard";

    // Передаваемые параметры
    private WindowManager windowManager;
    private KeyListener callback;
    private ViewGroup viewGroup;
    private boolean isFocused = false;

    private boolean isKeyboardActive = false;

    private final String[][] keyboard = new String[][] {
            {"1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "(", ")"},
            {"Й", "Ц", "У", "К", "Е", "Н", "Г", "Ш", "Щ", "З", "Х", "Ъ"},
            {"Ф", "Ы", "В", "А", "П", "Р", "О", "Л", "Д", "Ж", "Э"},
            {"Я", "Ч", "С", "М", "И", "Т", "Ь", "Б", "Ю"},
            {"Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P", "[", "]"},
            {"A", "S", "D", "F", "G", "H", "J", "K", "L", ";", "'"},
            {"Z", "X", "C", "V", "B", "N", "M", "<", ">"}
    };

    private Button firstKey, changeLanguage;
    private LinearLayout linearLayout;
    int navigationBarHeight;
    private Activity activity;

    // Параметры для установки размеров клавиатура
    private final int param = 12; // Ряд кнопок = 1/12 от высота экрана => клавиатура из 4х рядов занимает 1/3 экрана по высоте
    private final float sizeRation = 1.56F; // Высотка кнопки в портренной ориентации = ширине * sizeRation
    private int margin = 4; // Стандартный размер отступа между кнопками

    private final int spaceSize = 9; // Пробел по ширине как %spacesize% кнопок
    private View keyboardView;
    private int dpWidth, dpHeight;

    // Параметры клавиатуры
    private boolean numberLineEnabled;
    private boolean nightThemeEnabled;
    private boolean isRussian = true;
    private int orientation = 4;
    private Button capButton;
    private Window window;
    private Context context;

    public Keyboard(Context context, WindowManager windowManager, KeyListener callback, ViewGroup viewGroup, Window window, Activity activity) {
        super(context);
        this.callback = callback;
        this.viewGroup = viewGroup;
        this.windowManager = windowManager;
        this.window = window;
        this.context = context;
        this.activity = activity;
        OrientationEventListener orientationListener = new OrientationEventListener(context, SensorManager.SENSOR_DELAY_NORMAL) {
            @Override
            public void onOrientationChanged(int i) {
                int rotate = windowManager.getDefaultDisplay().getRotation();
                if (rotate != orientation) {
                    orientation = rotate;
                    //Log.e(TAG, "new orientation: " + orientation);
                    if (isKeyboardActive) {
                        hideKeyboard();
                        showKeyboard();
                    }
                }
            }
        };
        orientationListener.enable();
    }

    public void showKeyboard() {
        Display display = windowManager.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        float width = Float.parseFloat(String.valueOf(size.x));
        float height = Float.parseFloat(String.valueOf(size.y));
        margin = Math.round(width / 135) / 2;
        //dpWidth = Math.round(width / param) - margin * 2;
        dpWidth = (int) Math.floor(width / param) - margin * 2;
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.BOTTOM;

        if (width > height) { // Пейзаж
            dpHeight = Math.round(height / param) - margin * 2;
            if (isTablet(getContext())) {
                params.setMargins(0, 0,0, getSize() * 2);
            }
            else if (!isNavBarOnTheLeftSide()) {
                params.setMargins(0, 0, getSize(), 0);
            } else {
                params.setMargins(getSize(), 0, 0, 0);

            }
        } else { // Портрет
            dpHeight = Math.round(dpWidth * sizeRation) - margin * 2;
            if (isButtonsOnTheBottom()) {
                params.setMargins(0, 0, 0, getSize());
            } else {
                params.setMargins(0, getSize(), 0, 0);
            }
        }
        LayoutInflater inflater = LayoutInflater.from(getContext());
        if (numberLineEnabled) {
            keyboardView = inflater.inflate(R.layout.keyboard_view_nums, viewGroup, true);
        } else {
            keyboardView = inflater.inflate(R.layout.keyboard_view, viewGroup, true);
        }
        linearLayout = keyboardView.findViewById(R.id.button_1);
        linearLayout.setLayoutParams(params);
        keyboardView.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });

        ArrayList<String[]> array = new ArrayList<>();
        array.add(keyboard[0]);
        if (isRussian) {
            for (int i = 1; i < 4; i++) {
                array.add(keyboard[i]);
            }
        } else {
            for (int i = 4; i < 7; i++) {
                array.add(keyboard[i]);
            }
        }
        addKeys(array);
        isKeyboardActive = true;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return true;
    }

    public void setNightThemeEnabled(boolean nightThemeEnabled) {
        this.nightThemeEnabled = nightThemeEnabled;
    }

    private void addKeys(ArrayList<String[]> keyLines) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        LinearLayout host = keyboardView.findViewById(R.id.button_1);
        if (nightThemeEnabled) {
            host.setBackgroundColor(getResources().getColor(R.color.black));
        }
        LinearLayout linearLayout;
        int startIndex = numberLineEnabled ? 0 : 1;
        for (int i = startIndex; i < keyLines.size(); i++) {
            String[] array = keyLines.get(i);
            int pos = numberLineEnabled ? i : i - 1;
            linearLayout = keyboardView.findViewById(getLinearLayoutId(pos));

            if (isRussian && array.length == keyboard[3].length || !isRussian && array.length == keyboard[6].length) {
                View v = inflater.inflate(R.layout.keyboard_item, linearLayout, true);
                Button button = v.findViewById(R.id.key_button);
                changeLanguage = button;
                setLangunageButtonSize(button);
                if (isRussian) {
                    button.setText(R.string.ru_title);
                } else {
                    button.setText(R.string.eng_title);
                }
                button.setBackground(getResources().getDrawable(R.drawable.action_button_style));
                button.setOnClickListener(v1 -> { // Смена языка
                    boolean focused = changeLanguage.isFocused();
                    isRussian = !isRussian;
                    hideKeyboard();
                    showKeyboard();
                    if (focused) changeLanguage.requestFocusFromTouch();
                }); // Смена языка
                button.setId(R.id.button_1);
                if (nightThemeEnabled) {
                    button.setBackground(getResources().getDrawable(R.drawable.night_action_button_style));
                    button.setTextColor(getResources().getColor(R.color.white));
                } else {
                    button.setBackground(getResources().getDrawable(R.drawable.action_button_style));
                }
            }

            for (int j = 0; j < array.length; j++) {
                View v = inflater.inflate(R.layout.keyboard_item, linearLayout, true);
                Button b = v.findViewById(R.id.key_button);
                if (i == startIndex && j == 0) firstKey = b;
                b.setText(array[j]);
                b.setOnClickListener(v1 -> callback.onKeyClicked(b.getText().toString()));
                if (nightThemeEnabled) {
                    b.setTextColor(getResources().getColor(R.color.white));
                    b.setBackground(getResources().getDrawable(R.drawable.night_button_style));
                }
                setButtonSize(b);
                b.setId(R.id.button_1);
            }

            if (isRussian && array.length == keyboard[3].length || !isRussian && array.length == keyboard[6].length) {
                View v = inflater.inflate(R.layout.keyboard_clear, linearLayout, true);
                ImageButton button = v.findViewById(R.id.key_button);
                setClearSize(button);
                button.setBackgroundResource(R.drawable.action_button_style);
                button.setOnClickListener(v1 -> callback.onDeleteButtonClicked()); // Стереть 1 символ
                button.setOnLongClickListener(v1 -> {
                    callback.onLongDeleteButtonClicked();
                    return false;
                });
                button.setId(R.id.button_1);
                if (nightThemeEnabled) {
                    button.setBackground(getResources().getDrawable(R.drawable.night_action_button_style));
                    button.setColorFilter(getResources().getColor(R.color.white));
                } else {
                    button.setBackground(getResources().getDrawable(R.drawable.action_button_style));
                }
            }
        }
        linearLayout = keyboardView.findViewById(getLinearLayoutId(numberLineEnabled ? keyLines.size() : keyLines.size() - 1));
        View v = inflater.inflate(R.layout.keyboard_hide, linearLayout, true);
        ImageButton ib = v.findViewById(R.id.key_button);
        if (nightThemeEnabled) {
            ib.setBackground(getResources().getDrawable(R.drawable.night_action_button_style));
            ib.setColorFilter(getResources().getColor(R.color.white));
        } else {
            ib.setBackground(getResources().getDrawable(R.drawable.action_button_style));
        }
        ib.setOnClickListener(v1 -> {
            callback.onKeyboardHideClicked();
        });
        ib.setId(R.id.button_1);
        setClearSize(ib);
        // Добавление пробела
        v = inflater.inflate(R.layout.keyboard_space, linearLayout, true);
        Button b = v.findViewById(R.id.key_button);
        b.setOnClickListener(v1 -> callback.onKeyClicked(" "));
        setSpaceSize(b);
        b.setId(R.id.button_1);
        if (nightThemeEnabled) {
            b.setBackground(getResources().getDrawable(R.drawable.night_action_button_style));
        } else {
            b.setBackground(getResources().getDrawable(R.drawable.action_button_style));
        }
        v = inflater.inflate(R.layout.keyboard_search, linearLayout, true);
        ib = v.findViewById(R.id.key_button);
        if (nightThemeEnabled) {
            ib.setBackground(getResources().getDrawable(R.drawable.night_action_button_style));
            ib.setColorFilter(getResources().getColor(R.color.white));
        } else {
            ib.setBackground(getResources().getDrawable(R.drawable.action_button_style));
        }
        ib.setOnClickListener(v1 -> {
            callback.onKeyboardOkClicked();
        });
        setClearSize(ib);
    }

    public void setFocus() {
        firstKey.requestFocusFromTouch();
        isFocused = true;
    }

    public void setFocusOnBoard() {
        keyboardView.requestFocus();
        keyboardView.requestFocusFromTouch();
    }

    private void setLangunageButtonSize(Button button) {
        LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        params.height = dpHeight ;
        params.width = (int) Math.round(dpWidth * 1.5);
        params.leftMargin = margin;
        params.rightMargin = margin;
        params.bottomMargin = margin;
        params.topMargin = margin;
        button.setLayoutParams(params);
    }

    public int getLinearLayoutId(int index) {
        switch (index) {
            case 0: return R.id.firstLine;
            case 1: return R.id.secondLine;
            case 2: return R.id.thirdLine;
            case 3: return R.id.fourthLine;
            case 4: return R.id.fifthLine;
            default: throw new IndexOutOfBoundsException();
        }
    }

    private void setButtonSize(Button button) {
        LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        params.height = dpHeight;
        params.width = dpWidth;
        params.leftMargin = margin;
        params.rightMargin = margin;
        params.bottomMargin = margin;
        params.topMargin = margin;
        button.setLayoutParams(params);
    }

    private void setSpaceSize(Button button) {
        LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        params.height = dpHeight ;
        params.width = dpWidth * spaceSize;
        params.leftMargin = margin;
        params.rightMargin = margin;
        params.bottomMargin = margin;
        params.topMargin = margin;
        button.setLayoutParams(params);
    }

    private void setClearSize(ImageButton button) {
        LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        params.height = dpHeight ;
        params.width = (int) Math.round(dpWidth * 1.5);
        params.leftMargin = margin;
        params.rightMargin = margin;
        params.bottomMargin = margin;
        params.topMargin = margin;
        button.setLayoutParams(params);
    }

    public void hideKeyboard() {
        if (keyboardView != null) {
            View v = keyboardView.findViewById(R.id.button_1);
            viewGroup.removeView(v);
            //Log.e(TAG, "Keyboard was hidden");
        } else {
            //Log.e("Keyboard", "Keyboard is null!");
        }
        isKeyboardActive = false;
    }

    public interface KeyListener {
        void onKeyClicked(String key);
        void onDeleteButtonClicked();
        void onLongDeleteButtonClicked();
        void onKeyboardHideClicked();
        void onKeyboardOkClicked();
    }

    private void setNightMode(boolean status) {
        nightThemeEnabled = status;
    }

    private void setNumberLine(boolean status) {
        numberLineEnabled = status;
    }

    public static class Builder {

        private Context context;
        private WindowManager windowManager;
        private KeyListener callback;
        private ViewGroup viewGroup;
        private Window window;
        private Activity activity;
        private boolean nightMode = false;
        private boolean numberLine = false;

        public Builder(Activity activity, KeyListener callback) {
            this.context = activity.getApplicationContext();
            this.windowManager = activity.getWindowManager();
            this.callback = callback;
            this.viewGroup = (ViewGroup) activity.getWindow().getDecorView().getRootView();
            this.window = activity.getWindow();
            this.activity = activity;
        }

        public Builder setNightMode(boolean status) {
            nightMode = status;
            return this;
        }

        public Builder enableNumberLine(boolean status) {
            numberLine = status;
            return this;
        }

        public Keyboard build() {
            Keyboard keyboard = new Keyboard(context, windowManager, callback, viewGroup, window, activity);
            keyboard.setNightMode(nightMode);
            keyboard.setNumberLine(numberLine);
            return keyboard;
        }
    }


    private int getSize() { // Размер системных навигационных кнопок (высота)

        Rect rectangle = new Rect();
        window.getDecorView().getWindowVisibleDisplayFrame(rectangle);
        int statusBarHeight = rectangle.top;
        int contentViewTop =
                window.findViewById(Window.ID_ANDROID_CONTENT).getTop();
        int titleBarHeight= contentViewTop - statusBarHeight;
        //Log.e(TAG, "statusBarHeight: " + statusBarHeight);
        //Log.e(TAG, "titleBarHeight: " + titleBarHeight);

        int h = ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getHeight()
                - window.getDecorView().getRootView().getHeight();
        if (h == 0) {
            h = ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getWidth()
                    - window.getDecorView().getRootView().getWidth();
            //Log.e(TAG, "h: " + h);
            return Math.abs(h);

        }
        //Log.e(TAG, "h: " + h);


        return Math.abs(h) - statusBarHeight;

        /*ViewCompat.setOnApplyWindowInsetsListener(window.getDecorView().getRootView(), new androidx.core.view.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
                final int statusBarHeight = insets.getStableInsetBottom();
                Log.e(TAG, "statusBarHeight: " + statusBarHeight);
                return insets.consumeStableInsets();
            }
        });

        Resources resources = this.getResources();
        int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0) {
            Log.e(TAG, "bar height: " + resources.getDimensionPixelSize(resourceId));
            Log.e(TAG, "bar height: " + resources.getDimension(resourceId));
            Log.e(TAG, "bar height: " + resources.getDimensionPixelOffset(resourceId));
            Log.e(TAG, "bar height new: " + convertDpToPixel(resources.getDimension(resourceId), context));
            return resources.getDimensionPixelSize(resourceId);
        }
        return 0;*/
    }

    public boolean isNavBarOnTheLeftSide() {

        Rect outRect = new Rect();
        ViewGroup decor = (ViewGroup) window.getDecorView();
        decor.getWindowVisibleDisplayFrame(outRect);
        DisplayMetrics dm = getResources().getDisplayMetrics();
        Log.e(TAG, "value: " + (dm.widthPixels == outRect.bottom));


        int rotation =  windowManager.getDefaultDisplay().getRotation();
        int reqOr = activity.getRequestedOrientation();

        String aVerReleaseStr = Build.VERSION.RELEASE;
        int dotInd = aVerReleaseStr.indexOf(".");
        if (dotInd >= 0) {
            aVerReleaseStr = aVerReleaseStr.replaceAll("\\.", "");
            aVerReleaseStr = new StringBuffer(aVerReleaseStr).insert(dotInd, ".").toString();
        }

        float androidVer = Float.parseFloat(aVerReleaseStr);
        //Log.e(TAG, "androidVer: " + androidVer + " reqOr: " + reqOr);

        if (rotation == 3 && reqOr == 6) {
            //Log.e(TAG, "buttons on the left");
            return true;
        } else {
            //Log.e(TAG, "buttons on the right");
            return false;
        }
    }

    public static boolean isTablet(Context mContext){
        return (mContext.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    private boolean isButtonsOnTheRight() {
        int rotate = windowManager.getDefaultDisplay().getRotation();
        switch (rotate) {
            case Surface.ROTATION_90:
                return true;
            default:
                return false;
        }
    }

    private boolean isButtonsOnTheBottom() {
        int rotate = windowManager.getDefaultDisplay().getRotation();
        switch (rotate) {
            case Surface.ROTATION_0:
                return true;
            default:
                return false;
        }
    }
}