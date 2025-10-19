package com.example.keyboard3;

import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.view.View;
import android.view.MotionEvent;
import android.util.Log;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.PopupWindow;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import android.view.LayoutInflater;
import android.view.Gravity;
import android.graphics.Rect;
import android.provider.Settings;
import android.widget.Toast;
import android.os.Handler;
import android.text.TextUtils;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import java.util.ArrayList;
import java.util.List;

public class CustomKeyboard extends InputMethodService
        implements KeyboardView.OnKeyboardActionListener {

    private KeyboardView keyboardView;
    private Keyboard keyboard;
    private Keyboard russianKeyboard;
    private ClipboardManager clipboardManager;
    private List<String> clipboardHistory = new ArrayList<>();
    private PopupWindow clipboardPopup;
    private static final int MAX_CLIPBOARD_HISTORY = 3;

    // Координаты для перемещения пробела
    private float initialX, initialY;
    private boolean isSpaceMoving = false;
    private boolean isShiftPressed = false;
    private boolean isRussianLayout = false;

    // Улучшенное удаление
    private boolean isBackspacePressed = false;
    private Handler backspaceHandler = new Handler();
    private Runnable backspaceRunnable;
    private long lastBackspaceTime = 0;

    @Override
    public View onCreateInputView() {
        keyboardView = (KeyboardView) getLayoutInflater().inflate(R.layout.keyboard_view, null);

        // Загружаем обе раскладки
        keyboard = new Keyboard(this, R.xml.qwerty);
        russianKeyboard = new Keyboard(this, R.xml.qwerty_russian);

        keyboardView.setKeyboard(keyboard);
        keyboardView.setOnKeyboardActionListener(this);
        keyboardView.setPreviewEnabled(false);

        // Инициализируем обработчик буфера обмена
        clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

        // Инициализируем обработчик удаления
        initBackspaceHandler();

        // Настраиваем перемещение курсора через пробел
        setupSpaceBarMovement();

        return keyboardView;
    }

    private void initBackspaceHandler() {
        backspaceRunnable = new Runnable() {
            @Override
            public void run() {
                if (isBackspacePressed) {
                    long currentTime = System.currentTimeMillis();
                    // Проверяем что прошло достаточно времени с последнего удаления
                    if (currentTime - lastBackspaceTime > 150) {
                        if (hasSelectedText()) {
                            getCurrentInputConnection().commitText("", 1);
                        } else {
                            // УДАЛЯЕМ СТРОГО ОДИН СИМВОЛ
                            getCurrentInputConnection().deleteSurroundingText(1, 0);
                        }
                        lastBackspaceTime = currentTime;
                    }
                    backspaceHandler.postDelayed(this, 150);
                }
            }
        };
    }

    private boolean hasSelectedText() {
        ExtractedText extractedText = getCurrentInputConnection()
                .getExtractedText(new ExtractedTextRequest(), 0);
        return extractedText != null && extractedText.selectionStart != extractedText.selectionEnd;
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void setupSpaceBarMovement() {
        keyboardView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                float x = event.getX();
                float y = event.getY();

                List<Keyboard.Key> keys = keyboardView.getKeyboard().getKeys();
                Keyboard.Key spaceKey = null;

                for (Keyboard.Key key : keys) {
                    if (key.codes[0] == 32) {
                        spaceKey = key;
                        break;
                    }
                }

                if (spaceKey == null) return false;

                Rect spaceRect = new Rect(
                        spaceKey.x,
                        spaceKey.y,
                        spaceKey.x + spaceKey.width,
                        spaceKey.y + spaceKey.height
                );

                boolean isTouchingSpace = spaceRect.contains((int)x, (int)y);

                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        if (isTouchingSpace) {
                            initialX = x;
                            initialY = y;
                            isSpaceMoving = true;
                            return true;
                        }
                        break;

                    case MotionEvent.ACTION_MOVE:
                        if (isSpaceMoving && isTouchingSpace) {
                            float deltaX = x - initialX;

                            if (Math.abs(deltaX) > 40) {
                                if (deltaX > 0) {
                                    moveCursorRight();
                                } else {
                                    moveCursorLeft();
                                }
                                initialX = x;
                                return true;
                            }
                        }
                        break;

                    case MotionEvent.ACTION_UP:
                        if (isSpaceMoving) {
                            isSpaceMoving = false;
                            if (Math.abs(x - initialX) < 30 && Math.abs(y - initialY) < 30 && isTouchingSpace) {
                                getCurrentInputConnection().commitText(" ", 1);
                            }
                            return true;
                        }
                        break;
                }
                return false;
            }
        });
    }

    private void moveCursorLeft() {
        try {
            ExtractedText extractedText = getCurrentInputConnection()
                    .getExtractedText(new ExtractedTextRequest(), 0);
            if (extractedText != null) {
                int currentPosition = extractedText.selectionStart;
                int newPosition = Math.max(0, currentPosition - 1);
                getCurrentInputConnection().setSelection(newPosition, newPosition);
            }
        } catch (Exception e) {
            getCurrentInputConnection().setSelection(
                    getCurrentInputConnection().getCursorCapsMode(0) - 1,
                    getCurrentInputConnection().getCursorCapsMode(0) - 1
            );
        }
    }

    private void moveCursorRight() {
        try {
            ExtractedText extractedText = getCurrentInputConnection()
                    .getExtractedText(new ExtractedTextRequest(), 0);
            if (extractedText != null) {
                int currentPosition = extractedText.selectionStart;
                int textLength = extractedText.text != null ? extractedText.text.length() : 0;
                int newPosition = Math.min(textLength, currentPosition + 1);
                getCurrentInputConnection().setSelection(newPosition, newPosition);
            }
        } catch (Exception e) {
            getCurrentInputConnection().setSelection(
                    getCurrentInputConnection().getCursorCapsMode(0) + 1,
                    getCurrentInputConnection().getCursorCapsMode(0) + 1
            );
        }
    }

    private void switchToRussianLayout() {
        if (!isRussianLayout) {
            keyboardView.setKeyboard(russianKeyboard);
            isRussianLayout = true;
            showToast("RU");
        }
    }

    private void switchToEnglishLayout() {
        if (isRussianLayout) {
            keyboardView.setKeyboard(keyboard);
            isRussianLayout = false;
            showToast("EN");
        }
    }

    private void toggleShift() {
        isShiftPressed = !isShiftPressed;
        Keyboard currentKeyboard = keyboardView.getKeyboard();

        for (Keyboard.Key key : currentKeyboard.getKeys()) {
            if (key.codes[0] == -1) {
                key.on = isShiftPressed;
                break;
            }
        }
        keyboardView.invalidateAllKeys();
    }

    // ПРОСТОЙ BACKSPACE - ТОЛЬКО ОДИН СИМВОЛ
    private void handleBackspace() {
        long currentTime = System.currentTimeMillis();
        // Защита от двойного срабатывания
        if (currentTime - lastBackspaceTime < 200) {
            return;
        }

        if (hasSelectedText()) {
            getCurrentInputConnection().commitText("", 1);
        } else {
            // СТРОГО ОДИН СИМВОЛ
            getCurrentInputConnection().deleteSurroundingText(1, 0);
        }
        lastBackspaceTime = currentTime;
    }

    private void startBackspace() {
        isBackspacePressed = true;
        // Первое удаление
        handleBackspace();
        // Запускаем повторение с большим интервалом
        backspaceHandler.postDelayed(backspaceRunnable, 300);
    }

    private void stopBackspace() {
        isBackspacePressed = false;
        backspaceHandler.removeCallbacks(backspaceRunnable);
    }

    private void showClipboardPopup(View anchorView) {
        updateClipboardHistory();

        if (clipboardHistory.isEmpty()) {
            showToast("Буфер обмена пуст");
            return;
        }

        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.clipboard_popup, null);

        if (clipboardPopup != null && clipboardPopup.isShowing()) {
            clipboardPopup.dismiss();
        }

        clipboardPopup = new PopupWindow(popupView, 400, 450, true);

        ListView listView = popupView.findViewById(R.id.clipboard_list);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_list_item_1, clipboardHistory
        ) {
            @Override
            public View getView(int position, View convertView, android.view.ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                android.widget.TextView textView = (android.widget.TextView) view;

                String text = getItem(position);
                if (text.length() > 40) {
                    text = text.substring(0, 40) + "...";
                }
                textView.setText(text);
                textView.setPadding(30, 25, 30, 25);
                textView.setTextSize(14);

                return view;
            }
        };

        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedText = clipboardHistory.get(position);
            if (selectedText != null && !selectedText.trim().isEmpty()) {
                getCurrentInputConnection().commitText(selectedText, 1);
            }
            if (clipboardPopup != null) {
                clipboardPopup.dismiss();
                clipboardPopup = null;
            }
        });

        clipboardPopup.showAtLocation(anchorView, Gravity.TOP, 0, 150);
    }

    private void updateClipboardHistory() {
        try {
            if (clipboardManager != null && clipboardManager.hasPrimaryClip()) {
                ClipData clipData = clipboardManager.getPrimaryClip();
                if (clipData != null && clipData.getItemCount() > 0) {
                    CharSequence newText = clipData.getItemAt(0).getText();
                    if (newText != null && !TextUtils.isEmpty(newText)) {
                        String textStr = newText.toString().trim();
                        if (!textStr.isEmpty()) {
                            clipboardHistory.remove(textStr);
                            clipboardHistory.add(0, textStr);
                            while (clipboardHistory.size() > MAX_CLIPBOARD_HISTORY) {
                                clipboardHistory.remove(clipboardHistory.size() - 1);
                            }
                        }
                    }
                }
            }

            if (clipboardHistory.isEmpty()) {
                clipboardHistory.add("Скопируйте текст чтобы он появился здесь");
                clipboardHistory.add("Второй элемент истории");
                clipboardHistory.add("Третий элемент истории");
            }

        } catch (Exception e) {
            Log.e("Keyboard", "Error accessing clipboard", e);
        }
    }

    @Override
    public void onPress(int primaryCode) {
        if (primaryCode == -100) {
            showClipboardPopup(keyboardView);
        }

        if (primaryCode == Keyboard.KEYCODE_DELETE) {
            startBackspace();
        }
    }

    @Override
    public void onRelease(int primaryCode) {
        if (primaryCode == Keyboard.KEYCODE_DELETE) {
            stopBackspace();
        }
    }

    @Override
    public void onKey(int primaryCode, int[] keyCodes) {
        if (primaryCode == Keyboard.KEYCODE_DELETE) {
            // ОДИНОЧНОЕ НАЖАТИЕ - защита от двойного срабатывания
            handleBackspace();
        } else if (primaryCode == Keyboard.KEYCODE_DONE) {
            getCurrentInputConnection().commitText("\n", 1);
        } else if (primaryCode == -1) {
            toggleShift();
        } else if (primaryCode == -10) {
            if (isRussianLayout) {
                switchToEnglishLayout();
            } else {
                switchToRussianLayout();
            }
        } else if (primaryCode == -100) {
            // Обрабатывается в onPress
        } else {
            char code = (char) primaryCode;
            String text = String.valueOf(code);

            if (isShiftPressed) {
                text = text.toUpperCase();
                isShiftPressed = false;
                toggleShift();
            }

            getCurrentInputConnection().commitText(text, 1);
        }
    }

    @Override
    public void onText(CharSequence text) {
    }

    @Override
    public void swipeLeft() {
    }

    @Override
    public void swipeRight() {
    }

    @Override
    public void swipeDown() {
    }

    @Override
    public void swipeUp() {
    }
}