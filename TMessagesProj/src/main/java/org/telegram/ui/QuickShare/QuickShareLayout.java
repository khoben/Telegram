package org.telegram.ui.QuickShare;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.Emoji;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.ChatMessageCell;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.AlertsCreator;
import org.telegram.ui.Components.LayoutHelper;

import java.util.ArrayList;
import java.util.List;

@SuppressLint("ViewConstructor")
public class QuickShareLayout extends FrameLayout {

    private static final int LIMIT_DIALOGS = 5;
    private ArrayList<TLRPC.Dialog> dialogs = new ArrayList<TLRPC.Dialog>(LIMIT_DIALOGS);

    private int currentAccount;
    private ChatActivity fragment;
    private Theme.ResourcesProvider resourcesProvider;
    private MessageObject currentMessageObject;

    private TextView textBadgeLayout;
    private TextPaint textBadgePaint;
    private LinearLayout dialogsLayout;
    private Paint backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Drawable shadow;

    private Paint backgroundBadgePaint;
    private Paint darkenBackgroundBadgePaint;
    private boolean isFromBottom = false;

    @SuppressLint("AppCompatCustomView")
    public QuickShareLayout(@NonNull Context context, ChatActivity fragment, Theme.ResourcesProvider resourcesProvider, int currentAccount) {
        super(context);
        this.fragment = fragment;
        this.resourcesProvider = resourcesProvider;
        this.currentAccount = currentAccount;

        backgroundPaint.setColor(Theme.getColor(Theme.key_actionBarDefaultSubmenuBackground, resourcesProvider));
        shadow = ContextCompat.getDrawable(context, R.drawable.reactions_bubble_shadow).mutate();
        shadow.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_chat_messagePanelShadow), PorterDuff.Mode.MULTIPLY));

        textBadgePaint = (TextPaint) getThemedPaint(Theme.key_paint_chatActionText);
        textBadgeLayout = new TextView(context) {
            private final RectF drawingRect = new RectF();

            @Override
            public void setText(CharSequence text, BufferType type) {
                text = Emoji.replaceEmoji(text, getPaint().getFontMetricsInt(), dp(10), false);
                super.setText(text, type);
            }

            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
                drawingRect.set(0, 0, getMeasuredWidth(), getMeasuredHeight());
            }

            @Override
            protected void onDraw(Canvas canvas) {
                drawBadgeBackground(canvas);
                super.onDraw(canvas);
            }

            private void drawBadgeBackground(Canvas canvas) {
                backgroundBadgePaint = getThemedPaint(Theme.key_paint_chatActionBackground);
                darkenBackgroundBadgePaint = getThemedPaint(Theme.key_paint_chatActionBackgroundDarken);

                if (QuickShareLayout.this.resourcesProvider != null) {
                    QuickShareLayout.this.resourcesProvider.applyServiceShaderMatrix(getMeasuredWidth(), QuickShareLayout.this.getMeasuredHeight(), getX(), getY());
                } else {
                    Theme.applyServiceShaderMatrix(getMeasuredWidth(), QuickShareLayout.this.getMeasuredHeight(), getX(), getY());
                }

                canvas.drawRoundRect(drawingRect, dp(16), dp(16), QuickShareLayout.this.backgroundBadgePaint);

                if (hasGradientService()) {
                    canvas.drawRoundRect(drawingRect, dp(16), dp(16), QuickShareLayout.this.darkenBackgroundBadgePaint);
                }
            }
        };
        NotificationCenter.listenEmojiLoading(textBadgeLayout);
        textBadgeLayout.setTextSize(12);
        textBadgeLayout.setMaxLines(1);
        textBadgeLayout.setEllipsize(TextUtils.TruncateAt.END);
        textBadgeLayout.setTypeface(AndroidUtilities.bold());
        textBadgeLayout.setTextColor(textBadgePaint.getColor());
        textBadgeLayout.setPadding(dp(8), dp(2), dp(8), dp(2));
        textBadgeLayout.setGravity(Gravity.CENTER);
        textBadgeLayout.setMaxEms(12);
        textBadgeLayout.setVisibility(View.GONE);
        addView(textBadgeLayout, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        dialogsLayout = new LinearLayout(context) {
            private final RectF drawingRect = new RectF();

            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
                drawingRect.set(dp(7), dp(7), getMeasuredWidth() - dp(7), getMeasuredHeight() - dp(7));
                shadow.setBounds(0, 0, getMeasuredWidth(), getMeasuredHeight());
            }

            @Override
            protected void onDraw(Canvas canvas) {
                shadow.draw(canvas);
                canvas.drawRoundRect(drawingRect, dp(72), dp(72), QuickShareLayout.this.backgroundPaint);
                super.onDraw(canvas);
            }
        };
        dialogsLayout.setWillNotDraw(false);
        dialogsLayout.setPadding(dp(12), dp(6), dp(12), dp(6));
        dialogsLayout.setOrientation(LinearLayout.HORIZONTAL);
        dialogsLayout.setGravity(Gravity.CENTER);
        addView(dialogsLayout, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        setFocusable(true);
        setClickable(true);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        requestDisallowInterceptTouchEvent();
        if (event.getEventTime() - event.getDownTime() < 100 && event.getActionMasked() == MotionEvent.ACTION_UP) {
            hide();
            return false;
        }
        if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
            float x = event.getRawX();
            updateDialogViewsOnMove(x);
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            float x = event.getRawX();
            updateDialogViewsOnMove(x);
            if (selectedIdx != -1) {
                TLRPC.Dialog dialog = dialogs.get(selectedIdx);
                sendToInternal(currentMessageObject, dialog.id, true);
            } else {
                hide();
                return false;
            }
        } else if (event.getAction() == MotionEvent.ACTION_CANCEL) {
            textBadgeLayout.setVisibility(GONE);
        }
        return true;
    }

    private int selectedIdx = -1;
    private void updateDialogViewsOnMove(float x) {
        boolean found = false;
        selectedIdx = -1;
        for(int i = 0; i < dialogsLayout.getChildCount(); i++) {
            View view = dialogsLayout.getChildAt(i);
            float viewX = dialogsLayout.getX() + view.getX();
            if (x > viewX - 10 && x < viewX + dp(48) + 10) {
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
                showName(dialogsLayout.getX() + view.getX(), dialogsLayout.getY() + view.getY(), dialogs.get(i).id);
                found = true;
                selectedIdx = i;
                break;
            }
        }
        for(int i = 0; i < dialogsLayout.getChildCount(); i++) {
            View view = dialogsLayout.getChildAt(i);
            if (!found) {
                view.setScaleX(1f);
                view.setScaleY(1f);
                view.setAlpha(1f);
            } else {
                if (i == selectedIdx) {
                    view.setScaleX(1.1f);
                    view.setScaleY(1.1f);
                    view.setAlpha(1f);
                } else {
                    view.setScaleX(1f);
                    view.setScaleY(1f);
                    view.setAlpha(0.8f);
                }
            }
        }
        if (!found) {
            textBadgeLayout.setVisibility(GONE);
        }
    }

    private void fetchDialogs() {
        dialogs.clear();
        long selfUserId = UserConfig.getInstance(currentAccount).clientUserId;
        if (!MessagesController.getInstance(currentAccount).dialogsForward.isEmpty()) {
            TLRPC.Dialog dialog = MessagesController.getInstance(currentAccount).dialogsForward.get(0);
            dialogs.add(dialog);
        }
        ArrayList<TLRPC.Dialog> archivedDialogs = new ArrayList<TLRPC.Dialog>();
        ArrayList<TLRPC.Dialog> allDialogs = MessagesController.getInstance(currentAccount).getAllDialogs();
        for (int i = 0, allDialogsSize = allDialogs.size(); i < allDialogsSize; i++) {
            TLRPC.Dialog dialog = allDialogs.get(i);
            if (!(dialog instanceof TLRPC.TL_dialog)) {
                continue;
            }
            if (dialog.id == selfUserId) {
                continue;
            }
            if (DialogObject.isEncryptedDialog(dialog.id)) {
                continue;
            }
            if (DialogObject.isUserDialog(dialog.id)) {
                if (dialog.folder_id == 1) {
                    archivedDialogs.add(dialog);
                } else {
                    dialogs.add(dialog);
                }
            } else {
                TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(-dialog.id);
                if (!(chat == null || ChatObject.isNotInChat(chat) || chat.gigagroup && !ChatObject.hasAdminRights(chat) || ChatObject.isChannel(chat) && !chat.creator && (chat.admin_rights == null || !chat.admin_rights.post_messages) && !chat.megagroup)) {
                    if (dialog.folder_id == 1) {
                        archivedDialogs.add(dialog);
                    } else {
                        dialogs.add(dialog);
                    }
                }
            }
            if (dialogs.size() >= LIMIT_DIALOGS) {
                break;
            }
        }
        if (dialogs.size() < LIMIT_DIALOGS) {
            for (int i = 0; i < LIMIT_DIALOGS - dialogs.size() && i < archivedDialogs.size(); i++) {
                dialogs.add(archivedDialogs.get(i));
            }
        }
        dialogsLayout.removeAllViews();
        for (TLRPC.Dialog dialog : dialogs) {
            QuickShareDialogCell cell = new QuickShareDialogCell(getContext(), resourcesProvider, currentAccount);
            cell.setDialog(dialog.id);
            dialogsLayout.addView(cell, LayoutHelper.createLinear(48, 48));
        }
    }

    public void showFor(ChatMessageCell cell) {
        ViewGroup contentView = fragment.contentView;
        if (contentView == null) return;
        ViewGroup chatList = fragment.getChatListView();
        if (chatList == null) return;

        fetchDialogs();
        if (dialogs.isEmpty()) return;

        isNeedToIntercept = true;
        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);

        currentMessageObject = cell.getMessageObject();
        int showY = (int) (cell.getY() + cell.getSideButtonY() - dp(64) - AndroidUtilities.statusBarHeight - ActionBar.getCurrentActionBarHeight() + (fragment.isKeyboardVisible() ? -fragment.contentView.getKeyboardHeight() : 0));
        int showX = (int) (cell.getX() + cell.getSideButtonX());

        float dialogLayoutWidth = dialogs.size() * dp(48) + dp(24);
        float dialogLayoutX = showX - dialogLayoutWidth / 2;
        if (dialogLayoutX + dialogLayoutWidth > contentView.getMeasuredWidth() - dp(16)) {
            dialogLayoutX = contentView.getMeasuredWidth() - dp(16) - dialogLayoutWidth;
        }
        dialogsLayout.setX(dialogLayoutX);

        isFromBottom = false;
        if (showY < dp(8)) {
            showY = dp(8);
            isFromBottom = true;
        }

        dialogsLayout.setY(showY);

        setVisibility(VISIBLE);
    }

    public void hide() {
        dialogs.clear();
        isNeedToIntercept = false;
        setVisibility(GONE);
        currentMessageObject = null;
        selectedIdx = -1;
    }

    public boolean isShowing() {
        return getVisibility() == VISIBLE;
    }

    private boolean isNeedToIntercept = false;
    public boolean needToIntercept() {
        return isNeedToIntercept;
    }

    private void requestDisallowInterceptTouchEvent() {
        ViewGroup contentView = fragment.contentView;
        ViewGroup chatList = fragment.getChatListView();
        if (chatList != null) {
            chatList.requestDisallowInterceptTouchEvent(true);
        }
        if (contentView != null) {
            contentView.requestDisallowInterceptTouchEvent(true);
        }
    }

    private void showName(float x, float y, long dialogId) {
        if (DialogObject.isUserDialog(dialogId)) {
            TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(dialogId);
            if (UserObject.isReplyUser(user)) {
                textBadgeLayout.setText(LocaleController.getString(R.string.RepliesTitle));
            } else if (UserObject.isUserSelf(user)) {
                textBadgeLayout.setText(LocaleController.getString(R.string.SavedMessages));
            } else {
                if (user != null) {
                    textBadgeLayout.setText(user.first_name);
                } else {
                    textBadgeLayout.setText("");
                }
            }
        } else {
            TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(-dialogId);
            if (chat != null) {
                textBadgeLayout.setText(chat.title);
            } else {
                textBadgeLayout.setText("");
            }
        }
        textBadgeLayout.measure(0, 0);
        float viewWidth = textBadgeLayout.getMeasuredWidth();
        float viewX = x + dp(24) - viewWidth / 2f;
        if (viewX + viewWidth > fragment.contentView.getMeasuredWidth() - dp(8)) {
            viewX = fragment.contentView.getMeasuredWidth() - dp(8) - viewWidth;
        }

        textBadgeLayout.setX(viewX);
        textBadgeLayout.setY(isFromBottom ? y + dp(68) : y - dp(28));
        textBadgeLayout.setVisibility(VISIBLE);
    }

    private void sendToInternal(MessageObject messageObject, long dialogId, boolean withSound) {
        if (messageObject == null) return;
        ArrayList<MessageObject> message = new ArrayList<>(1);
        message.add(messageObject);
        int result = SendMessagesHelper.getInstance(currentAccount).sendMessage(message, dialogId, false, false, withSound, 0, null);
        if (result != 0) {
            AlertsCreator.showSendMediaAlert(result, fragment, resourcesProvider);
        } else {
            fragment.showQuickShareForwarded(dialogId);
        }
        hide();
    }

    private Paint getThemedPaint(String paintKey) {
        Paint paint = resourcesProvider != null ? resourcesProvider.getPaint(paintKey) : null;
        return paint != null ? paint : Theme.getThemePaint(paintKey);
    }

    private boolean hasGradientService() {
        return (resourcesProvider != null ? resourcesProvider.hasGradientService() : Theme.hasGradientService());
    }

    public boolean isAffected(List<Integer> messagesId) {
        return currentMessageObject != null && messagesId.contains(currentMessageObject.getId());
    }
}
