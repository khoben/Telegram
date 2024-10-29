package org.telegram.ui.QuickShare;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

import org.telegram.messenger.DialogObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AnimatedFloat;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.Premium.PremiumGradient;

@SuppressLint("ViewConstructor")
public class QuickShareDialogCell extends FrameLayout implements NotificationCenter.NotificationCenterDelegate {

    private final BackupImageView imageView;
    private final AvatarDrawable avatarDrawable = new AvatarDrawable() {
        @Override
        public void invalidateSelf() {
            super.invalidateSelf();
            imageView.invalidate();
        }
    };
    private TLRPC.User user;

    private float onlineProgress;
    private long lastUpdateTime;
    private long currentDialog;

    public final Theme.ResourcesProvider resourcesProvider;
    private final int currentAccount;


    private final AnimatedFloat premiumBlockedT = new AnimatedFloat(this, 0, 350, CubicBezierInterpolator.EASE_OUT_QUINT);
    private boolean premiumBlocked;

    public boolean isBlocked() {
        return premiumBlocked;
    }

    public BackupImageView getImageView() {
        return imageView;
    }

    public QuickShareDialogCell(Context context, Theme.ResourcesProvider resourcesProvider, int currentAccount) {
        super(context);
        this.resourcesProvider = resourcesProvider;
        this.currentAccount = currentAccount;

        setWillNotDraw(false);

        imageView = new BackupImageView(context);
        imageView.setRoundRadius(dp(28));
        addView(imageView, LayoutHelper.createFrame(38, 38, Gravity.CENTER));
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.userIsPremiumBlockedUpadted);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.userIsPremiumBlockedUpadted);
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.userIsPremiumBlockedUpadted) {
            final boolean wasPremiumBlocked = premiumBlocked;
            premiumBlocked = user != null && MessagesController.getInstance(currentAccount).isUserPremiumBlocked(user.id);
            if (premiumBlocked != wasPremiumBlocked) {
                invalidate();
            }
        }
    }


    public void setDialog(long uid) {
        if (DialogObject.isUserDialog(uid)) {
            user = MessagesController.getInstance(currentAccount).getUser(uid);
            premiumBlocked = MessagesController.getInstance(currentAccount).isUserPremiumBlocked(uid);
            premiumBlockedT.set(premiumBlocked, true);
            invalidate();
            avatarDrawable.setInfo(currentAccount, user);
            if (UserObject.isReplyUser(user)) {
                avatarDrawable.setAvatarType(AvatarDrawable.AVATAR_TYPE_REPLIES);
                avatarDrawable.setScaleSize(.8f);
                imageView.setImage(null, null, avatarDrawable, user);
            } else if (UserObject.isUserSelf(user)) {
                avatarDrawable.setAvatarType(AvatarDrawable.AVATAR_TYPE_SAVED);
                avatarDrawable.setScaleSize(.8f);
                imageView.setImage(null, null, avatarDrawable, user);
            } else {
                imageView.setForUserOrChat(user, avatarDrawable);
            }
            imageView.setRoundRadius(dp(28));
        } else {
            user = null;
            premiumBlocked = false;
            premiumBlockedT.set(0, true);
            TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(-uid);
            avatarDrawable.setInfo(currentAccount, chat);
            imageView.setForUserOrChat(chat, avatarDrawable);
            imageView.setRoundRadius(chat != null && chat.forum ? dp(16) : dp(28));
        }
        currentDialog = uid;
    }

    public long getCurrentDialog() {
        return currentDialog;
    }

    private PremiumGradient.PremiumGradientTools premiumGradient;
    private Drawable lockDrawable;

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        boolean result = super.drawChild(canvas, child, drawingTime);
        if (child == imageView) {
            if (user != null && !MessagesController.isSupportUser(user)) {
                long newTime = SystemClock.elapsedRealtime();
                long dt = newTime - lastUpdateTime;
                if (dt > 17) {
                    dt = 17;
                }
                lastUpdateTime = newTime;

                final float lockT = premiumBlockedT.set(premiumBlocked);
                if (lockT > 0) {
                    int top = imageView.getBottom() - dp(9);
                    int left = imageView.getRight() - dp(9.33f);

                    canvas.save();
                    Theme.dialogs_onlineCirclePaint.setColor(getThemedColor(Theme.key_windowBackgroundWhite));
                    canvas.drawCircle(left, top, dp(12) * lockT, Theme.dialogs_onlineCirclePaint);
                    if (premiumGradient == null) {
                        premiumGradient = new PremiumGradient.PremiumGradientTools(Theme.key_premiumGradient1, Theme.key_premiumGradient2, -1, -1, -1, resourcesProvider);
                    }
                    premiumGradient.gradientMatrix(left - dp(10), top - dp(10), left + dp(10), top + dp(10), 0, 0);
                    canvas.drawCircle(left, top, dp(10) * lockT, premiumGradient.paint);
                    if (lockDrawable == null) {
                        lockDrawable = getContext().getResources().getDrawable(R.drawable.msg_mini_lock2).mutate();
                        lockDrawable.setColorFilter(new PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN));
                    }
                    lockDrawable.setBounds(
                            (int) (left - lockDrawable.getIntrinsicWidth() / 2f * .875f * lockT),
                            (int) (top - lockDrawable.getIntrinsicHeight() / 2f * .875f * lockT),
                            (int) (left + lockDrawable.getIntrinsicWidth() / 2f * .875f * lockT),
                            (int) (top + lockDrawable.getIntrinsicHeight() / 2f * .875f * lockT)
                    );
                    lockDrawable.setAlpha((int) (0xFF * lockT));
                    lockDrawable.draw(canvas);
                    canvas.restore();
                } else {
                    boolean isOnline = !premiumBlocked && !user.self && !user.bot && (user.status != null && user.status.expires > ConnectionsManager.getInstance(currentAccount).getCurrentTime() || MessagesController.getInstance(currentAccount).onlinePrivacy.containsKey(user.id));
                    if (isOnline || onlineProgress != 0) {
                        int top = imageView.getBottom() - dp(6);
                        int left = imageView.getRight() - dp(10);
                        Theme.dialogs_onlineCirclePaint.setColor(getThemedColor(Theme.key_windowBackgroundWhite));
                        canvas.drawCircle(left, top, dp(7) * onlineProgress, Theme.dialogs_onlineCirclePaint);
                        Theme.dialogs_onlineCirclePaint.setColor(getThemedColor(Theme.key_chats_onlineCircle));
                        canvas.drawCircle(left, top, dp(5) * onlineProgress, Theme.dialogs_onlineCirclePaint);
                        if (isOnline) {
                            if (onlineProgress < 1.0f) {
                                onlineProgress += dt / 150.0f;
                                if (onlineProgress > 1.0f) {
                                    onlineProgress = 1.0f;
                                }
                                imageView.invalidate();
                                invalidate();
                            }
                        } else {
                            if (onlineProgress > 0.0f) {
                                onlineProgress -= dt / 150.0f;
                                if (onlineProgress < 0.0f) {
                                    onlineProgress = 0.0f;
                                }
                                imageView.invalidate();
                                invalidate();
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    private int getThemedColor(int key) {
        return Theme.getColor(key, resourcesProvider);
    }
}