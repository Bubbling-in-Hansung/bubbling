package bubbling.client.ui;

import bubbling.client.LiveSession;
import bubbling.client.ui.dialog.NotificationCards;
import bubbling.common.dto.chat.ChatParticipantDto;
import bubbling.common.dto.chat.ChatPositionDto;
import bubbling.common.dto.game.GameRankingResponse;
import bubbling.common.dto.user.UserDto;
import bubbling.common.enums.GameType;
import bubbling.client.ui.model.*;
import bubbling.client.ui.game.GameFrame;
import bubbling.client.ui.game.GameView;
import bubbling.client.ui.screen.*;
import bubbling.client.ui.drawing.CharacterArt;
import bubbling.client.ui.style.UiFonts;
import static bubbling.client.ui.model.UiPages.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.IntSupplier;
import static bubbling.client.ui.style.Styles.*;

/** Root owns screen transitions. Screens never depend on transport objects. */
public final class ApplicationView extends JPanel {
    private static final int HEADER_PADDING_Y = 6;
    private static final int HEADER_HEIGHT = 62 + 2 * HEADER_PADDING_Y;
    private static final int HEADER_ACCOUNT_RIGHT_PADDING = 36;
    private static final int COMPACT_HEADER_WIDTH = 1100;
    private static final Color PROFILE_BG = color("f4f0ff"), PROFILE_BORDER = color("e8e0ff");
    private final CardLayout pages = new CardLayout();
    private final JPanel deck = new JPanel(pages);
    private final HomeScreen home;
    private final ChatScreen chat;
    private final CardLayout headerCards = new CardLayout();
    private final JPanel headerContent = panel(headerCards);
    private final CardLayout shellPages = new CardLayout();
    private final JPanel shellDeck = new JPanel(shellPages);
    private String currentPage = "";
    private JButton homeButton, chatButton, gameButton;
    private GameFrame gameFrame;
    private GameView gameView;
    private Window observedMainWindow;
    private final Set<String> seenMemberEvents = new HashSet<>();
    private final Map<String, String> unopenedRoomNotices = new HashMap<>();
    private final Map<String, Integer> unreadNoticesByRoom = new HashMap<>();
    private final Map<String, Integer> unreadMessagesByRoom = new HashMap<>();
    private int unreadIncomingRequests;
    private final UiData data;
    private final LiveSession live;
    private final AuthScreen login;
    private final AuthScreen signup;
    private final ProfileScreen profileScreen;
    private final ConversationListScreen conversations;
    private JLabel nickname;
    private JLabel caption;

    public ApplicationView(UiData data, LiveSession live) {
        super(new BorderLayout());
        this.data = data;
        this.live = live;
        home = new HomeScreen(data, this::enterRoom, this::clearIncomingRequestBadge);
        chat = new ChatScreen(data, () -> showPage(HOME));
        JPanel shell = panel(new BorderLayout());
        shell.add(createHeader(), BorderLayout.NORTH);
        shell.add(createSidebar(), BorderLayout.WEST);
        shellDeck.add(home, HOME);
        conversations = new ConversationListScreen(data, this::enterRoom, this::roomNotificationDescription);
        shellDeck.add(conversations, CONVERSATIONS);
        headerContent.add(title("나의 대화", 14), CONVERSATIONS);
        profileScreen = new ProfileScreen(data, live::updateProfile);
        shellDeck.add(profileScreen, PROFILE);
        shell.add(shellDeck);
        deck.add(shell, "shell");
        deck.add(chat, CHAT);
        login = new AuthScreen(false, () -> showPage(SIGNUP),
                (id, password, name) -> live.authenticate(false, id, password, name));
        signup = new AuthScreen(true, () -> showPage(LOGIN),
                (id, password, name) -> live.authenticate(true, id, password, name));
        deck.add(login, LOGIN);
        deck.add(signup, SIGNUP);
        add(deck);
        live.bind(this, data.session());
        authentication(false, "서버에 연결 중입니다…");
        showPage(LOGIN);
    }

    private JPanel createHeader() {
        JPanel header = panel(new BorderLayout());
        header.setOpaque(true);
        header.setBackground(Color.WHITE);
        header.setPreferredSize(new Dimension(0, HEADER_HEIGHT));
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, LINE),
                BorderFactory.createEmptyBorder(HEADER_PADDING_Y, 0, HEADER_PADDING_Y, 0)));
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
        headerContent.add(title("마이페이지", 14), PROFILE);
        header.add(headerContent);
        JPanel user = panel(new GridBagLayout());
        user.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, HEADER_ACCOUNT_RIGHT_PADDING));
        user.setPreferredSize(new Dimension(207 + HEADER_ACCOUNT_RIGHT_PADDING, 62));
        JComponent dot = new JComponent() {
            protected void paintComponent(Graphics source) {
                paintStatusDot(source, 16, 4, 8, true);
            }
        };
        dot.setPreferredSize(new Dimension(16, 16));
        JPanel account = column();
        nickname = title("로그인 후 이용해주세요", 14);
        nickname.setAlignmentX(Component.RIGHT_ALIGNMENT);
        account.add(nickname);
        caption = label("", 11, MUTED);
        caption.setAlignmentX(Component.RIGHT_ALIGNMENT);
        account.add(Box.createVerticalStrut(2));
        account.add(caption);
        JPanel userInfo = panel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        userInfo.add(dot);
        userInfo.add(Box.createHorizontalStrut(9));
        userInfo.add(account);
        GridBagConstraints userConstraints = new GridBagConstraints();
        userConstraints.weightx = 1;
        userConstraints.anchor = GridBagConstraints.EAST;
        user.add(userInfo, userConstraints);
        header.add(user, BorderLayout.EAST);
        header.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent e) {
                boolean compact = header.getWidth() <= COMPACT_HEADER_WIDTH;
                brand.setPreferredSize(new Dimension(compact ? 180 : 206, 62));
                user.setPreferredSize(new Dimension((compact ? 167 : 207) + HEADER_ACCOUNT_RIGHT_PADDING, 62));
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
        sidebar.setPreferredSize(new Dimension(86, 0));
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
        homeButton.setAlignmentX(Component.CENTER_ALIGNMENT);
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
        chatButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        top.add(chatButton);
        top.add(Box.createVerticalStrut(10));
        gameButton = button("게임", false, this::openGameWindow);
        gameButton.setIcon(sidebarIcon("game", () -> 0));
        gameButton.setMargin(new Insets(0, 0, 0, 0));
        gameButton.setVerticalTextPosition(SwingConstants.BOTTOM);
        gameButton.setHorizontalTextPosition(SwingConstants.CENTER);
        gameButton.setFont(font(9, false));
        gameButton.putClientProperty("flat", true);
        gameButton.setPreferredSize(new Dimension(58, 56));
        gameButton.setMaximumSize(new Dimension(58, 56));
        gameButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        gameButton.getAccessibleContext().setAccessibleName("게임");
        top.add(gameButton);
        sidebar.add(top, BorderLayout.NORTH);
        JPanel bottom = column();
        pad(bottom, 10);
        JButton profile = new JButton() {
            protected void paintComponent(Graphics source) {
                var g = graphics(source);
                g.setColor(PROFILE_BG);
                g.fillOval(0, 0, 39, 39);
                g.setColor(PROFILE_BORDER);
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
        profile.setAlignmentX(Component.CENTER_ALIGNMENT);
        JButton logout = button("", false, live::logout);
        logout.setIcon(icon("logout"));
        logout.putClientProperty("flat", true);
        String logoutLabel = "로그아웃";
        logout.setToolTipText(logoutLabel);
        logout.getAccessibleContext().setAccessibleName(logoutLabel);
        append(bottom, logout, 5);
        logout.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebar.add(bottom, BorderLayout.SOUTH);
        return sidebar;
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
            unreadNoticesByRoom.clear();
            updateChatBadge();
            conversations.onEnter();
            if (changedPage) {
                live.requestChatRooms();
            }
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
        if (page.equals(PROFILE) && changedPage) {
            live.requestAccount();
        }
    }

    public String currentPage() {
        return currentPage;
    }

    public void enterRoom(RoomView room) {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("Use EDT");
        }
        java.util.Objects.requireNonNull(room);
        var state = data.session().find(room);
        if (state == null) return;
        if (state.conversation != null && (state.joined || state.ended)) {
            openLiveChat(state);
            if (state.listed) live.requestHistory(state.conversation.chatRoomId());
        } else {
            data.session().openLiveRoom(state);
        }
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
                g.setColor(BADGE_RED);
                g.fillRoundRect(badgeX, y, width, 15, 15, 15);
                g.setColor(Color.WHITE);
                g.drawString(count, badgeX + (width - metrics.stringWidth(count)) / 2,
                        y + (15 - metrics.getHeight()) / 2 + metrics.getAscent());
                g.dispose();
            }
        };
    }

    public void requestsLoaded(int count) {
        if (count <= 0) return;
        unreadIncomingRequests += count;
        updateIncomingRequestBadge();
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
        recordChatNotice(approvalId, room, "새로 수락된 참여 요청", "참여 요청이 수락됐어요",
                "‘" + room.view.title() + "’ 대화를 채팅 목록에서 열 수 있어요.");
    }

    public void memberJoined(ChatParticipantDto participant, UiSession.Room room) {
        if (participant == null || room == null || room.view.privateRoom()) return;
        String detail = participant.nickname() == null || participant.nickname().isBlank()
                ? "‘" + room.view.title() + "’ 대화에 새 참여자가 들어왔어요."
                : "‘" + room.view.title() + "’ 대화에 " + participant.nickname() + "님이 들어왔어요.";
        recordChatNotice(participant.chatMemberId(), room, "새 참여자 입장", "새 참여자가 들어왔어요", detail);
    }

    private void recordChatNotice(String eventId, UiSession.Room room, String rowNotice,
            String title, String detail) {
        if (eventId != null && !seenMemberEvents.add(eventId)) return;
        if (!(currentPage.equals(CHAT) && chat.isShowingRoom(room.id))) {
            unopenedRoomNotices.put(approvalRoomId(room), rowNotice);
            unreadNoticesByRoom.merge(room.id, 1, Integer::sum);
            updateChatBadge();
        }
        NotificationCards.info(this, title, detail);
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
        String roomNotice = unopenedRoomNotices.get(approvalRoomId(room));
        Integer unreadMessages = unreadMessagesByRoom.get(room.id);
        if (unreadMessages == null) return roomNotice;
        String description = "읽지 않은 메시지 " + unreadMessages + "건";
        return roomNotice == null ? description : roomNotice + ", " + description;
    }

    private int chatBadgeCount() {
        return unreadNoticesByRoom.values().stream().mapToInt(Integer::intValue).sum()
                + unreadMessagesByRoom.values().stream().mapToInt(Integer::intValue).sum();
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
        data.session().applyAccount(user);
        nickname.setText(user == null ? "" : user.nickname() + "님");
        profileScreen.setAccount(user);
        if (gameView != null) {
            gameView.syncProfileCharacter(CharacterChoice.at(data.session().character()));
        }
        repaint();
    }

    public void openGameWindow() {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("Use EDT");
        }
        attachMainWindowCloseHook();
        if (gameFrame != null && gameFrame.isDisplayable() && gameView != null) {
            gameView.syncProfileCharacter(CharacterChoice.at(data.session().character()));
            gameView.refreshBestScores();
            gameFrame.setExtendedState(Frame.NORMAL);
            gameFrame.setVisible(true);
            gameFrame.toFront();
            gameFrame.requestFocus();
            return;
        }
        GameView createdView = new GameView(data.session(), this::closeGameWindow);
        GameFrame createdFrame = new GameFrame(createdView, SwingUtilities.getWindowAncestor(this));
        createdFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                if (gameView == createdView) {
                    createdView.stopAllGames();
                    gameView = null;
                }
                if (gameFrame == createdFrame) {
                    gameFrame = null;
                }
            }
        });
        gameView = createdView;
        gameFrame = createdFrame;
        createdView.refreshBestScores();
        createdFrame.setVisible(true);
    }

    public void closeGameWindow() {
        GameFrame frame = gameFrame;
        GameView view = gameView;
        gameFrame = null;
        gameView = null;
        if (view != null) {
            view.stopAllGames();
        }
        if (frame != null && frame.isDisplayable()) {
            frame.dispose();
        }
    }

    private void attachMainWindowCloseHook() {
        Window window = SwingUtilities.getWindowAncestor(this);
        if (window == null || window == observedMainWindow) return;
        observedMainWindow = window;
        window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                closeGameWindow();
            }

            @Override
            public void windowClosed(WindowEvent e) {
                closeGameWindow();
            }
        });
    }

    public void gameResultSaved() {
        if (gameView != null) {
            gameView.onResultSaved();
        }
    }

    public void gameRankingLoaded(GameType gameType, GameRankingResponse response) {
        if (gameView != null) {
            gameView.onRankingLoaded(gameType, response);
        }
    }

    public Component notificationTarget() {
        return gameFrame != null && gameFrame.isShowing() && gameFrame.isActive() && gameView != null
                ? gameView
                : this;
    }

    public void passwordChanged() {
        NotificationCards.info(this, "비밀번호 변경 완료", "비밀번호가 성공적으로 변경되었어요.");
    }

    public void setOnline(int count) {
        caption.setText("현재 접속 " + count + "명");
    }

    public void openLiveChat(UiSession.Room room) {
        data.session().visit(room);
        chat.showRoom(room.view);
        unopenedRoomNotices.remove(approvalRoomId(room));
        unreadNoticesByRoom.remove(room.id);
        unreadMessagesByRoom.remove(room.id);
        updateChatBadge();
        showPage(CHAT);
    }

    public void returnFromChat(String bubbleId) {
        if (currentPage.equals(CHAT) && chat.isShowingRoom(bubbleId)) showPage(HOME);
    }

    public void characterMoved(ChatPositionDto position) {
        if (currentPage.equals(CHAT)) {
            chat.updateCharacterPosition(position);
        }
    }

    public void refreshLiveState() {
        if (currentPage.equals(HOME)) home.refresh();
        if (currentPage.equals(CONVERSATIONS)) conversations.refresh();
        if (currentPage.equals(CHAT)) {
            chat.refreshLive();
        }
    }

    public void resetLiveState() {
        closeGameWindow();
        seenMemberEvents.clear();
        unopenedRoomNotices.clear();
        unreadNoticesByRoom.clear();
        unreadMessagesByRoom.clear();
        updateChatBadge();
        clearIncomingRequestBadge();
        NotificationCards.clear(this);
        showPage(LOGIN);
        data.session().resetLiveState();
        home.resetLive();
        chat.resetLive();
        updateAccount(null);
        caption.setText("");
    }
}
