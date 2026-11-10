import bubbling.client.config.ClientConfig;
import bubbling.client.network.ClientMessageDispatcher;
import bubbling.client.network.TcpClient;
import bubbling.client.ui.frame.MainFrame;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

// 클라이언트 최초 초기화
public class ClientBootstrap {
    
    // volatile로 가시성 확보
    // synchronized로 원자성 확보는 필요시 활용 
    private volatile boolean closing = false;
    
    public void start() {
    
        // 기존 스윙 테마 대신 FlatLightLaf 테마 사용
        FlatLightLaf.setup();

        // 초기 Config 대상 : 서버 주소 및 포트 정보
        // 추가 Config 대상 : UI 설정 정보 등
        ClientConfig config = new ClientConfig();

        // 서버에서 수신한 메시지(응답 및 브로드캐스트)를 타입별로 분배
        // 1차 작업 : payload를 지정한 DTO 객체로 변환 (fromPayload)
        // 2차 작업 : SwingUtilities.invokeLater를 통해 Swing EDT로 DTO 객체 전달
        ClientMessageDispatcher dispatcher = new ClientMessageDispatcher();

        // 클라이언트 TCP 소켓 통신 관리
        TcpClient client = new TcpClient(
                config.getHost(),
                config.getPort(),
                dispatcher
        );

        // 스윙 UI 출력
        SwingUtilities.invokeLater(() -> {
            // MainFrame Root 형식으로 띄우기
            MainFrame frame = new MainFrame(config);
            frame.setDefaultCloseOperation(
                    JFrame.DISPOSE_ON_CLOSE
            );

            // 윈도우 이벤트 리스너
            frame.addWindowListener(new WindowAdapter() {
                @Override
                // 창을 닫으면 volatile 상태 변경(닫힘) 후 클라이언트 TCP 연결 해제 후 자원 반환
                public void windowClosed(WindowEvent e) {
                    closing = true;
                    client.close();
                }
            });

            // 메인 프레임 view 출력
            frame.setVisible(true);
        });
        
        // 데몬 스레드로 연결 자체만 담당하는 스레드 생성 후 최초 1회만 실행
        Thread connectionThread = new Thread(() -> {
            try {
                client.clientConnect();
                if (closing) {
                    client.close();
                }
            } catch (IOException e) {
                if (closing) {
                    return;
                }
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(
                            null,
                            "서버 연결에 실패했습니다.\n" + e.getMessage(),
                            "연결 오류",
                            JOptionPane.ERROR_MESSAGE
                    );
                });
            }

        }, "tcp-connect");

        connectionThread.setDaemon(true);
        connectionThread.start();
    }
}
