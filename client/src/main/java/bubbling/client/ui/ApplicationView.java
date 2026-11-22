package bubbling.client.ui;

import bubbling.client.LiveSession;
import bubbling.client.ui.dialog.NotificationCards;
import bubbling.common.dto.user.UserDto;
import bubbling.client.ui.model.*;
import bubbling.client.ui.screen.*;
import bubbling.client.ui.drawing.CharacterArt;
import bubbling.client.ui.style.UiFonts;
import static bubbling.client.ui.model.UiPages.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.IntSupplier;
import static bubbling.client.ui.style.Styles.*;

/** Root owns screen transitions. Screens never depend on transport objects. */
public final class ApplicationView extends JPanel {
    private final CardLayout pages = new CardLayout();
    private final JPanel deck = new JPanel(pages);
    private final HomeScreen home;
    private final ChatScreen chat;
    private final CardLayout headerCards = new CardLayout();
    private final JPanel headerContent = panel(headerCards);
    private final CardLayout shellPages = new CardLayout();
    private final JPanel shellDeck = new JPanel(shellPages);
    private String currentPage = "";
    private JButton homeButton, chatButton;
    private final Set<String> seenApprovals = new HashSet<>();
    private final Set<String> unopenedApprovedRooms = new HashSet<>();
    private final Map<String, Integer> unreadMessagesByRoom = new HashMap<>();
    private int unreadApprovals;
    private int unreadIncomingRequests;
    private RoomView currentRoom;
    private final UiData data;
    private final LiveSession live;
    private final AuthScreen login;
    private final AuthScreen signup;
    private final ProfileScreen profileScreen;
    private final ConversationListScreen conversations;
    private JLabel nickname;
    private JLabel caption;

    public ApplicationView(UiData data) {
        this(data, null);
    }

    public ApplicationView(UiData data, LiveSession live) {
        super(new BorderLayout());
        this.data = data;
        this.live = live;
        home = new HomeScreen(data, this::enterRoom, this::requestApproved, this::clearIncomingRequestBadge);
        chat = new ChatScreen(data, () -> showPage(HOME));
        JPanel shell = panel(new BorderLayout());
        shell.add(createHeader(), BorderLayout.NORTH);
        shell.add(createSidebar(), BorderLayout.WEST);
        shellDeck.add(home, HOME);
        conversations = new ConversationListScreen(data, this::enterRoom, this::roomNotificationDescription);
        shellDeck.add(conversations, CONVERSATIONS);
        headerContent.add(title("나의 대화", 14), CONVERSATIONS);
        profileScreen = new ProfileScreen(data, live == null ? null : live::updateProfile);
        shellDeck.add(profileScreen, PROFILE);
        shell.add(shellDeck);
        deck.add(shell, "shell");
        deck.add(chat, CHAT);
        Runnable previewHome = live == null ? () -> showPage(HOME) : null;
        login = new AuthScreen(false, () -> showPage(SIGNUP), previewHome,
                live == null ? null : (id, password, name) -> live.authenticate(false, id, password, name));
        signup = new AuthScreen(true, () -> showPage(LOGIN), previewHome,
                live == null ? null : (id, password, name) -> live.authenticate(true, id, password, name));
        deck.add(login, LOGIN);
        deck.add(signup, SIGNUP);
        add(deck);
        if (live != null) {
            live.bind(this, data.session());
            authentication(false, "서버에 연결 중입니다…");
        }
        showPage(live == null ? HOME : LOGIN);
    }

    private JPanel createHeader() {
        JPanel header = panel(new BorderLayout());
        header.setOpaque(true);
        header.setBackground(Color.WHITE);
        header.setPreferredSize(new Dimension(0, 62));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, LINE));
        JComponent brand = new JComponent() {
            protected void paintComponent(Graphics s) {
                Graphics2D g = graphics(s);
                logo(g, 25, 15, PURPLE, 21);
                g.dispose();
            }
        };
        brand.setPreferredSize(new Dimension(206, 62));
        header.add(brand, BorderLayout.WEST);
        JPanel tabs = panel(new FlowLayout(FlowLayout.CENTER, 7, 13));
        for (String name : java.util.stream.Stream.concat(java.util.stream.Stream.of("전체"), java.util.Arrays.stream(bubbling.common.enums.BubbleCategory.values()).map(bubbling.common.enums.BubbleCategory::getLabel)).toArray(String[]::new)) {
            JButton tab = categoryTab(name, () -> home.filter(name));
            selectCategoryTab(tab, name.equals("전체"));
            tab.addActionListener(e -> {
                for (Component c : tabs.getComponents()) {
                    if (c instanceof JButton b) {
                        selectCategoryTab(b, b == tab);
                    }
                }
            });
            tabs.add(tab);
        }
        JScrollPane categories = new JScrollPane(tabs);
        categories.setBorder(null);
        categories.setOpaque(false);
        categories.getViewport().setOpaque(true);
        categories.getViewport().setBackground(Color.WHITE);
        categories.getHorizontalScrollBar().setUnitIncrement(24);
        categories.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        categories.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        headerContent.add(categories, HOME);
        headerContent.add(title("마이페이지    나의 프로필과 계정 정보를 확인해요", 14), PROFILE);
        header.add(headerContent);
        JPanel user = panel(new FlowLayout(FlowLayout.RIGHT, 9, 0));
        user.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 23));
        user.setPreferredSize(new Dimension(230, 62));
        JComponent dot = new JComponent() {
            protected void paintComponent(Graphics source) {
                var g = graphics(source);
                g.setColor(color("e7f9f3"));
                g.fillOval(0, 0, 16, 16);
                g.setColor(color("52d4aa"));
                g.fillOval(4, 4, 8, 8);
                g.dispose();
            }
        };
        dot.setPreferredSize(new Dimension(16, 16));
        user.add(dot);
        JPanel account = column();
        nickname = label(data.preview() ? "버블버블" : "로그인 후 이용해주세요", 11, INK);
        nickname.setFont(font(11, true));
        nickname.setAlignmentX(Component.RIGHT_ALIGNMENT);
        account.add(nickname);
        caption = label(data.preview() ? "미리보기" : "", 8, color("9d98a7"));
        caption.setAlignmentX(Component.RIGHT_ALIGNMENT);
        account.add(Box.createVerticalStrut(2));
        account.add(caption);
        user.add(account);
        header.add(user, BorderLayout.EAST);
        header.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent e) {
                boolean compact = header.getWidth() <= 1100;
                brand.setPreferredSize(new Dimension(compact ? 180 : 206, 62));
                user.setPreferredSize(new Dimension(compact ? 190 : 230, 62));
                for (Component c : tabs.getComponents()) {
                    if (c instanceof JButton b) {
                        b.setBorder(BorderFactory.createEmptyBorder(8, compact ? 10 : 15, 8, compact ? 10 : 15));
                    }
                }
                header.revalidate();
            }
        });
        return header;
    }

    private JPanel createSidebar() {
        JPanel sidebar = panel(new BorderLayout());
        sidebar.setOpaque(true);
        sidebar.setBackground(Color.WHITE);
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, LINE));
        sidebar.setPreferredSize(new Dimension(72, 0));
        JPanel top = column();
        top.setBorder(BorderFactory.createEmptyBorder(20, 7, 0, 7));
        homeButton = button("홈", false, () -> showPage(HOME));
        homeButton.setIcon(sidebarIcon(HOME, () -> unreadIncomingRequests));
        homeButton.setMargin(new Insets(0, 0, 0, 0));
        homeButton.setVerticalTextPosition(SwingConstants.BOTTOM);
        homeButton.setHorizontalTextPosition(SwingConstants.CENTER);
        homeButton.setFont(font(9, false));
        homeButton.putClientProperty("flat", true);
        homeButton.setSelected(true);
        homeButton.setPreferredSize(new Dimension(58, 56));
        homeButton.setMaximumSize(new Dimension(58, 56));
        top.add(homeButton);
        top.add(Box.createVerticalStrut(10));
        chatButton = button("채팅", false, () -> showPage(CONVERSATIONS));
        chatButton.setIcon(sidebarIcon(CHAT, this::chatBadgeCount));
        chatButton.setVerticalTextPosition(SwingConstants.BOTTOM);
        chatButton.setHorizontalTextPosition(SwingConstants.CENTER);
        chatButton.putClientProperty("flat", true);
        chatButton.setMargin(new Insets(0, 0, 0, 0));
        chatButton.setFont(UiFonts.body(9, 700));
        chatButton.setPreferredSize(new Dimension(58, 56));
        chatButton.setMaximumSize(new Dimension(58, 56));
        top.add(chatButton);
        sidebar.add(top, BorderLayout.NORTH);
        JPanel bottom = column();
        pad(bottom, 10);
        JButton profile = new JButton() {
            protected void paintComponent(Graphics source) {
                var g = graphics(source);
                g.setColor(color("f4f0ff"));
                g.fillOval(0, 0, 39, 39);
                g.setColor(color("e8e0ff"));
                g.drawOval(0, 0, 39, 39);
                g.dispose();
                super.paintComponent(source);
            }
        };
        profile.setContentAreaFilled(false);
        profile.setBorderPainted(false);
        profile.setFocusPainted(false);
        profile.addActionListener(e -> showPage(PROFILE));
        profile.setIcon(new Icon() {
            public int getIconWidth() {
                return 34;
            }
            public int getIconHeight() {
                return 40;
            }
            public void paintIcon(Component c, Graphics s, int x, int y) {
                Graphics2D g = graphics(s);
                CharacterArt.paint(g, x, y, .42, data.session().character());
                g.dispose();
            }
        });
        profile.putClientProperty("flat", true);
        profile.setMargin(new Insets(0, 0, 0, 0));
        profile.setPreferredSize(new Dimension(40, 40));
        profile.setMaximumSize(new Dimension(40, 40));
        profile.setToolTipText("마이페이지");
        profile.getAccessibleContext().setAccessibleName("마이페이지");
        append(bottom, profile, 8);
        JButton logout = button("", false, this::leaveApplication);
        logout.setIcon(icon("logout"));
        logout.putClientProperty("flat", true);
        String logoutLabel = live == null ? "로그인 화면" : "로그아웃 및 종료";
        logout.setToolTipText(logoutLabel);
        logout.getAccessibleContext().setAccessibleName(logoutLabel);
        append(bottom, logout, 5);
        sidebar.add(bottom, BorderLayout.SOUTH);
        return sidebar;
    }

    private void leaveApplication() {
        if (live == null) {
            showPage(LOGIN);
            return;
        }
        Window window = SwingUtilities.getWindowAncestor(this);
        if (window != null) {
            window.dispatchEvent(new WindowEvent(window, WindowEvent.WINDOW_CLOSING));
        }
    }

    public void showPage(String page) {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("Use EDT");
        }
        if (!UiPages.contains(page)) {
            throw new IllegalArgumentException(page);
        }
        boolean changedPage = !page.equals(currentPage);
        currentPage = page;
        homeButton.setSelected(page.equals(HOME));
        chatButton.setSelected(page.equals(CONVERSATIONS));
        if (page.equals(HOME) && changedPage) {
            home.onEnter();
        }
        if (page.equals(CONVERSATIONS)) {
            unreadApprovals = 0;
            updateChatBadge();
            conversations.refresh();
        }
        if (UiPages.usesShell(page)) {
            pages.show(deck, "shell");
            shellPages.show(shellDeck, page);
            headerCards.show(headerContent, page);
        } else {
            pages.show(deck, page);
        }
        revalidate();
        repaint();
        if (live != null && page.equals(PROFILE) && changedPage) {
            live.requestAccount();
        }
    }

    public String currentPage() {
        return currentPage;
    }

    public RoomView currentRoom() {
        return currentRoom;
    }

    public void enterRoom(RoomView room) {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("Use EDT");
        }
        java.util.Objects.requireNonNull(room);
        var state = data.session().find(room);
        if (live != null) {
            if (state == null) return;
            if (state.conversation != null && (state.joined || state.ended)) {
                openLiveChat(state);
                if (state.listed) live.requestHistory(state.conversation.chatRoomId());
            } else {
                data.session().openLiveRoom(state);
            }
            return;
        }
        currentRoom = room;
        if (state != null) {
            data.session().visit(state);
        }
        chat.showRoom(room);
        if (state != null) unopenedApprovedRooms.remove(approvalRoomId(state));
        showPage(CHAT);
    }

    public void authentication(boolean enabled, String status) {
        login.authentication(enabled, status);
        signup.authentication(enabled, status);
    }

    public void signedIn(UserDto user) {
        updateAccount(user);
        showPage(HOME);
    }

    private static Icon sidebarIcon(String name, IntSupplier unread) {
        return new Icon() {
            private final Icon baseIcon = icon(name);

            public int getIconWidth() { return 38; }
            public int getIconHeight() { return 27; }

            public void paintIcon(Component source, Graphics sourceGraphics, int x, int y) {
                baseIcon.paintIcon(source, sourceGraphics, x + 8, y + 5);
                int unreadCount = unread.getAsInt();
                if (unreadCount == 0) return;
                Graphics2D g = graphics(sourceGraphics);
                String count = unreadCount > 99 ? "99+" : Integer.toString(unreadCount);
                g.setFont(font(8, true));
                FontMetrics metrics = g.getFontMetrics();
                int width = Math.max(15, metrics.stringWidth(count) + 8);
                int badgeX = x + getIconWidth() - width;
                g.setColor(Color.WHITE);
                g.fillRoundRect(badgeX - 1, y - 1, width + 2, 17, 17, 17);
                g.setColor(color("c93d60"));
                g.fillRoundRect(badgeX, y, width, 15, 15, 15);
                g.setColor(Color.WHITE);
                g.drawString(count, badgeX + (width - metrics.stringWidth(count)) / 2,
                        y + (15 - metrics.getHeight()) / 2 + metrics.getAscent());
                g.dispose();
            }
        };
    }

    public void requestReceived(String bubbleId) {
        unreadIncomingRequests++;
        updateIncomingRequestBadge();
        var room = data.session().room(bubbleId);
        String detail = room == null ? "참여 요청 관리에서 확인할 수 있어요."
                : "‘" + room.view.title() + "’에 새 참여 요청이 있어요.";
        NotificationCards.info(this, "참여 요청이 왔어요", detail);
    }

    public void requestApproved(String approvalId, UiSession.Room room) {
        if (!seenApprovals.add(approvalId)) return;
        unopenedApprovedRooms.add(approvalRoomId(room));
        unreadApprovals++;
        updateChatBadge();
        NotificationCards.info(this, "참여 요청이 수락됐어요",
                "‘" + room.view.title() + "’ 대화를 채팅 목록에서 열 수 있어요.");
    }

    private static String approvalRoomId(UiSession.Room room) {
        return room.conversation == null ? room.id : room.conversation.chatRoomId();
    }

    public void chatMessageReceived(UiSession.Room room) {
        if (currentPage.equals(CHAT) && chat.isShowingRoom(room.id)) return;
        unreadMessagesByRoom.merge(room.id, 1, Integer::sum);
        updateChatBadge();
    }

    private String roomNotificationDescription(UiSession.Room room) {
        boolean hasApproval = unopenedApprovedRooms.contains(approvalRoomId(room));
        Integer unreadMessages = unreadMessagesByRoom.get(room.id);
        if (unreadMessages == null) return hasApproval ? "새로 수락된 참여 요청" : null;
        String description = "읽지 않은 메시지 " + unreadMessages + "건";
        return hasApproval ? "새로 수락된 참여 요청, " + description : description;
    }

    private int chatBadgeCount() {
        return unreadApprovals + unreadMessagesByRoom.values().stream().mapToInt(Integer::intValue).sum();
    }

    private void updateChatBadge() {
        int count = chatBadgeCount();
        chatButton.getAccessibleContext().setAccessibleName(count == 0
                ? "채팅" : "채팅, 확인하지 않은 알림 " + count + "건");
        chatButton.repaint();
    }

    private void clearIncomingRequestBadge() {
        unreadIncomingRequests = 0;
        updateIncomingRequestBadge();
    }

    private void updateIncomingRequestBadge() {
        homeButton.getAccessibleContext().setAccessibleName(unreadIncomingRequests == 0
                ? "홈" : "홈, 확인하지 않은 참여 요청 " + unreadIncomingRequests + "건");
        homeButton.repaint();
    }

    public void updateAccount(UserDto user) {
        if (live != null) {
            data.session().applyProfile(user == null ? null : user.profile());
        }
        nickname.setText(user == null ? "" : user.nickname() + "님");
        profileScreen.setAccount(user);
        repaint();
    }

    public void setOnline(int count) {
        caption.setText("접속 " + count + "명");
    }

    public void openLiveChat(UiSession.Room room) {
        currentRoom = room.view;
        data.session().visit(room);
        chat.showRoom(room.view);
        unopenedApprovedRooms.remove(approvalRoomId(room));
        unreadMessagesByRoom.remove(room.id);
        updateChatBadge();
        showPage(CHAT);
    }

    public void returnFromChat(String bubbleId) {
        if (currentPage.equals(CHAT) && chat.isShowingRoom(bubbleId)) showPage(HOME);
    }

    public void refreshLiveState() {
        if (currentPage.equals(HOME)) home.refresh();
        if (currentPage.equals(CONVERSATIONS)) conversations.refresh();
        if (currentPage.equals(CHAT)) {
            chat.refreshLive();
        }
    }

    public void resetLiveState() {
        seenApprovals.clear();
        unopenedApprovedRooms.clear();
        unreadMessagesByRoom.clear();
        unreadApprovals = 0;
        updateChatBadge();
        clearIncomingRequestBadge();
        NotificationCards.clear(this);
        showPage(LOGIN);
        data.session().resetLiveState();
        currentRoom = null;
        home.resetLive();
        chat.resetLive();
        updateAccount(null);
        caption.setText("");
    }
}
