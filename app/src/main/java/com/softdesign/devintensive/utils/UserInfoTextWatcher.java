package com.softdesign.devintensive.utils;

import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.support.design.widget.TextInputLayout;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.softdesign.devintensive.R;

/**
 * Validates user info fields and formats user cellphone number into russian locale cellphone format
 */
public class UserInfoTextWatcher implements TextWatcher {

    private static final String TAG = ConstantManager.TAG_PREFIX + UserInfoTextWatcher.class
            .getSimpleName();
    private static final int MAX_DIGITS_COUNT = 11;
    private static final int MAX_SYMBOLS_COUNT = 15;
    private final Resources mResources;
    private final EditText mEditText;
    private final TextInputLayout mTextInputLayout;
    private final LinearLayout mLinearLayout;
    private Boolean handlerIsAlive = false;
    private int mTimerLength = ConstantManager.ET_ERROR_TIMER_LENGTH_NORMAL;

    public UserInfoTextWatcher(Context context, EditText editText, TextInputLayout textInputLayout, LinearLayout linearLayout) {
        this.mResources = context.getResources();
        this.mEditText = editText;
        this.mTextInputLayout = textInputLayout;
        this.mLinearLayout = linearLayout;
    }
    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }
    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }
    @Override
    public void afterTextChanged(Editable s) {
        switch (mEditText.getId()) {
            case R.id.phone_EditText:
                validateAndReformatPhone(s);
                break;
            case R.id.email_EditText:
                validateEmail(s);
                break;
            case R.id.vk_EditText:
                validateVK(s);
                break;
            case R.id.gitHub_EditText:
                validateGitHub(s);
                break;
        }
    }

    /**
     * displays or removes error at current TextInputLayout
     * @param isError - if true - displays an error, if false - removes
     * @param errorType - error message
     */
    private void errorHandler(Boolean isError, String errorType) {
        if (!mEditText.isEnabled() && !mEditText.isFocusable()) return;
        if (isError) {
            final LinearLayout.LayoutParams lp =
                    (LinearLayout.LayoutParams) mLinearLayout.getLayoutParams();
            lp.height = mResources.getDimensionPixelSize(R.dimen.size_large_88);
            mLinearLayout.setLayoutParams(lp);

            mTextInputLayout.setErrorEnabled(true);
            mTextInputLayout.setError(errorType);

            if (!handlerIsAlive) {
                handlerIsAlive = true;
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mTextInputLayout.setError(null);
                        mTextInputLayout.setErrorEnabled(false);
                        lp.height = mResources.getDimensionPixelSize(R.dimen.size_large_72);
                        mLinearLayout.setLayoutParams(lp);
                        handlerIsAlive = false;
                    }
                }, mTimerLength);
            }
        } else {
            LinearLayout.LayoutParams lp =
                    (LinearLayout.LayoutParams) mLinearLayout.getLayoutParams();
            lp.height = mResources.getDimensionPixelSize(R.dimen.size_large_72);
            mLinearLayout.setLayoutParams(lp);
            mTextInputLayout.setError(null);
            mTextInputLayout.setErrorEnabled(false);
        }
    }

    /**
     * validates and reformats cellphone number into ru.locale format
     * @param s editText text
     */
    private void validateAndReformatPhone(Editable s) {
        String phone = s.toString().trim();

        Boolean setCursorPosToEnd = false;
        Boolean isError = false;
        String formattedPhone = "";
        int cursorPosition = mEditText.getSelectionStart();
        String errorText;

        if (cursorPosition == phone.length()) {
            setCursorPosToEnd = true;
        }

        /////////// Validation section ///////////

        errorText = isValidPhone(phone);
        if (errorText != null) isError = true;

        /////////// Reformatting section ///////////

        int a = 0;
        if (phone.startsWith("+")) a = 1;
        //Убираем превышение по количеству символов
        if (phone.length() > MAX_SYMBOLS_COUNT + a) {
            if (setCursorPosToEnd || cursorPosition == 0 ||
                    (phone.startsWith("7") || phone.startsWith("8")) && cursorPosition == 1 ||
                    (phone.startsWith("(7") || phone.startsWith("(8")) && cursorPosition == 2) {
                cursorPosition = cursorPosition + 1;
                phone = phone.substring(0, MAX_SYMBOLS_COUNT + a); //отсекаем лишние символы в конце номера
            } else {
                //Удаляем только что введенный символ
                phone = phone.substring(0, cursorPosition - 1) + phone.substring(cursorPosition);
                cursorPosition -= 1;
            }
        }

        //Переформатируем номер в +7(ххх)ххх-хх-хх

        phone = phone.replaceAll("\\D", "");

        if (phone.length() > 0) {
            formattedPhone = getFormattedPhone(phone);
        }

        //Выставляем переформатированный номер в EditText и меняем положение курсора

        mEditText.removeTextChangedListener(this);
        mEditText.setText(formattedPhone);
        if (setCursorPosToEnd || cursorPosition > mEditText.getText().toString().length()) {
            mEditText.setSelection(mEditText.getText().toString().length());
        } else {
            mEditText.setSelection(cursorPosition);
        }
        mEditText.addTextChangedListener(this);
        errorHandler(isError, errorText);
    }

    /**
     * validates email if it matches format ***@**.**
     * @param s editText text
     */
    private void validateEmail(Editable s) {

        String email = s.toString().trim();
        Boolean isValid = isValidEmail(email);
        errorHandler(!isValid, mResources.getString(R.string.error_editText_email));
    }

    /**
     * cuts https(s) and validates vk link to match format vk.com/***
     * @param s editText text
     */
    private void validateVK(Editable s) {
        String vk = s.toString().trim().toLowerCase();
        Boolean isValid = false;
        int a;
        if ((a = vk.indexOf("vk.com")) != -1) {
            if (a != 0) {
                String newString = s.toString().trim().substring(a);
                mEditText.removeTextChangedListener(this);
                mEditText.setText(newString);
                mEditText.addTextChangedListener(this);
            } else {
                isValid = isValidVK(vk);
            }
        }
        errorHandler(!isValid, mResources.getString(R.string.error_editText_vk));
    }
    /**
     * cuts https(s) and validates gitHub link to match format gitHub.com/***
     * @param s editText text
     */
    private void validateGitHub(Editable s) {

        String gh = s.toString().trim().toLowerCase();
        Boolean isValid = false;
        int a;
        if ((a = gh.indexOf("github.com")) != -1) {
            if (a != 0) {
                String newString = s.toString().trim().substring(a);
                mEditText.removeTextChangedListener(this);
                mEditText.setText(newString);
                mEditText.addTextChangedListener(this);
            } else {
                isValid = isValidGitHub(gh);
            }
        }
        errorHandler(!isValid, mResources.getString(R.string.error_editText_gitHub));
    }

    /**
     * validates cellphone number if it matches ru.locale format
     * @param phone cellphone number
     * @return error message if phone is not valid; null if it is valid;
     */
    private String isValidPhone(String phone) {

        String checkPhone = phone.replaceAll("[\\(\\)\\-\\+]", "");

        //Ошибка: вводите только цифры
        if (checkPhone.matches("(\\d*\\D\\d*)*")) {
            mTimerLength = ConstantManager.ET_ERROR_TIMER_LENGTH_LONG;
            return mResources.getString(R.string.error_editText_phone_wrong_symbols);
        }

        //Ошибка: цифр в номере менее 11
        if (checkPhone.length() < MAX_DIGITS_COUNT) {
            mTimerLength = ConstantManager.ET_ERROR_TIMER_LENGTH_LONG;
            return mResources.getString(R.string.error_editText_phone_wrong_length);
        } else {
            //Ошибка: номер должен начинаться с цифры 7 или 8
            if (!checkPhone.startsWith("7") && !checkPhone.startsWith("8")) {
                mTimerLength = ConstantManager.ET_ERROR_TIMER_LENGTH_LONG;
                return mResources.getString(R.string.error_editText_phone_wrong_code);
            }
            //Ошибка: цифр в номере больше 11
            if (checkPhone.length() > MAX_DIGITS_COUNT) {
                mTimerLength = ConstantManager.ET_ERROR_TIMER_LENGTH_SHORT;
                return mResources.getString(R.string.error_editText_phone_wrong_length);
            }
        }
        return null;
    }

    /**
     *
     * @param email   email
     * @return true if it matches format ***@**.**
     */
    public static boolean isValidEmail(String email) {
        String pattern = "^[\\w\\+\\.\\%\\-]{3,256}\\@[a-zA-Z0-9][a-zA-Z0-9\\-]{1,64}(\\.[a-zA-Z0-9][a-zA-Z0-9\\-]{1,25})+$";
        return !TextUtils.isEmpty(email) && email.matches(pattern);
    }

    /**
     *
     * @param vk vk link
     * @return true if it matches format vk.com/***
     */
    public static boolean isValidVK(String vk) {
        String pattern = "^vk.com\\/\\w{3,256}$";
        return !TextUtils.isEmpty(vk) && vk.matches(pattern);
    }

    /**
     *
     * @param s gitHub link
     * @return true if it matches format gitHub.com/***
     */
    public static boolean isValidGitHub(String s) {
        String pattern = "^github.com\\/\\w{3,256}(\\/\\w+)?$";
        return !TextUtils.isEmpty(s) && s.matches(pattern);
    }

    /**
     * reformats cellphone number into +7(***)***-**-**
     * @param phone cellphone number
     * @return String reformatted phone
     */
    private String getFormattedPhone(String phone) {
        String countryCode = "";
        String mobileOperatorCode = "";
        String firstNumberPart = "";
        String secondNumberPart = "";
        String thirdNumberPart = "";

        int index = 0;
        //countryCode
        if (phone.startsWith("7") || phone.startsWith("8")) {
            countryCode = "+7";
            index = 1;
        }

        if (index < phone.length()) {
            phone = phone.substring(index);
            if (phone.length() <= 3) {
                mobileOperatorCode = phone.substring(0, phone.length());
            } else {
                mobileOperatorCode = phone.substring(0, 3);
                if (phone.length() <= 6) {
                    firstNumberPart = phone.substring(3, phone.length());
                } else {
                    firstNumberPart = phone.substring(3, 6);
                    if (phone.length() <= 8) {
                        secondNumberPart = phone.substring(6, phone.length());
                    } else {
                        secondNumberPart = phone.substring(6, 8);
                        thirdNumberPart = phone.substring(8, phone.length());
                    }
                }
            }
        }

        StringBuilder stringBuilder = new StringBuilder();
        if (countryCode.length() > 0) {
            stringBuilder.append(countryCode);
        }
        if (mobileOperatorCode.length() > 0) {
            stringBuilder.append("(");
            stringBuilder.append(mobileOperatorCode);
        }
        if (firstNumberPart.length() > 0) {
            stringBuilder.append(")");
            stringBuilder.append(firstNumberPart);
        }
        if (secondNumberPart.length() > 0) {
            stringBuilder.append("-");
            stringBuilder.append(secondNumberPart);
        }
        if (thirdNumberPart.length() > 0) {
            stringBuilder.append("-");
            stringBuilder.append(thirdNumberPart);
        }
        return stringBuilder.toString();
    }
}

