package org.example;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.animation.Timeline;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

public class CountDwn extends Application{

    private enum State{IDLE,RUNNING,PAUSED,FINISHED}
    private Label lblTimeLeft=new Label("00:00:00");
    private State state=State.IDLE;
    private Timeline timeline;
    private long remainingMillis=0;
    private long targetEndNanos=0;
    private Button btnStart=new Button("START");
    private Button btnPause=new Button("PAUSE");
    private Button btnCancel=new Button("CANCEL");

    TextField textFieldHH=new TextField();
    TextField textFieldMM=new TextField();
    TextField textFieldSS=new TextField();

    private MediaPlayer mediaPlayer;

    @Override
    public void start(Stage stage) throws Exception{
            stage.setTitle("CountDownTimer");
            stage.setHeight(300);
            stage.setWidth(400);

            //Media
            var url=java.util.Objects.requireNonNull(
                    getClass().getResource("/sounds/BubbleTea1.mp3")
            );
            Media media=new Media(url.toExternalForm());
            mediaPlayer=new MediaPlayer(media);
            mediaPlayer.setVolume(0.7);

            //Show Time Left
            lblTimeLeft.setFont(Font.font("Consolas",80));
            lblTimeLeft.setMaxWidth(Double.MAX_VALUE);
            lblTimeLeft.setAlignment(Pos.CENTER);

            HBox boxTimeLeft=new HBox();
            boxTimeLeft.setAlignment(Pos.CENTER);
            boxTimeLeft.setPadding(new Insets(10));
            boxTimeLeft.setSpacing(5);
            boxTimeLeft.getChildren().add(lblTimeLeft);

            //Show Hours
            Label lblhh=new Label("hh");
            lblhh.setFont(Font.font(15));
            textFieldHH.setPrefWidth(60);
            HBox boxHH=new HBox(textFieldHH,lblhh);

            //Show Minutes
            Label lblmm=new Label("mm");
            lblmm.setFont(Font.font(15));
            textFieldMM.setPrefWidth(60);
            HBox boxMM=new HBox(textFieldMM,lblmm);

            //Show Seconds
            Label lblss=new Label("ss");
            lblss.setFont(Font.font(15));
            textFieldSS.setPrefWidth(60);
            HBox boxSS=new HBox(textFieldSS,lblss);

            HBox boxTimes=new HBox(boxHH,boxMM,boxSS);
            boxTimes.setAlignment(Pos.CENTER);
            boxTimes.setPadding(new Insets(10));
            boxTimes.setSpacing(5);


            //Start,Pause,Cancel Buttons
            btnStart.setOnAction(e -> startFromInputs());
            btnPause.setOnAction(e -> pauseOrResume());
            btnCancel.setOnAction(e -> cancelAll());
            HBox boxBtn=new HBox();
            boxBtn.setAlignment(Pos.CENTER);
            boxBtn.setPadding(new Insets(10));
            boxBtn.setSpacing(5);
            boxBtn.getChildren().addAll(btnStart,btnPause,btnCancel);

            //VBox
            VBox root=new VBox();
            root.setAlignment(Pos.CENTER);
            root.setPadding(new Insets(5));
            root.setSpacing(5);
            root.getChildren().addAll(boxTimeLeft,boxTimes,boxBtn);

            stage.setScene(new Scene(root));
            stage.show();
    }
    private long parseToMillis(String hh,String mm,String ss){
        try{
            //處理空白情況
            String hStr=(hh==null||hh.isBlank())?"0":hh.trim();
            String mStr=(mm==null||mm.isBlank())?"0":mm.trim();
            String sStr=(ss==null||ss.isBlank())?"0":ss.trim();

            //轉換成整數
            int h=Integer.parseInt(hStr);
            int m=Integer.parseInt(mStr);
            int s=Integer.parseInt(sStr);

            //檢查合法範圍
            if(h<0||m<0||s<0||m>=60||s>=60){
                return -1;
            }
            //轉成毫秒long
            return (h*3600L+m*60L+s)*1000L;
        }catch(NumberFormatException e){
            //如果輸入不是數字
            return -1;
        }
    }
    private String formatHMS(long millis){
        long totalSec=Math.max(0,(millis+500)/1000);
        long hours=totalSec/3600;
        long minutes=(totalSec%3600)/60;
        long seconds=totalSec%60;
        return String.format("%02d:%02d:%02d",hours,minutes,seconds);
    }
    private void tick(){
        if(state!=State.RUNNING) return;

        long nowNanos=System.nanoTime();
        remainingMillis=Math.max(0,(targetEndNanos-nowNanos)/1000000L);

        lblTimeLeft.setText(formatHMS(remainingMillis));

        if(remainingMillis<=0){
            if(timeline!=null) timeline.stop();

            //播放音效
            if(mediaPlayer!=null){
                mediaPlayer.stop();//防止重疊
                mediaPlayer.play();
            }
            state=State.FINISHED;
            updateButtons();
        }
    }
    private void createOrResetTimeLine(){
        if(timeline!=null){
            timeline.stop();
        }
        timeline=new Timeline(
                new KeyFrame(Duration.seconds(1),e -> tick())
        );
        timeline.setCycleCount(Animation.INDEFINITE);
    }
    private void startFromInputs(){
        long ms=parseToMillis(textFieldHH.getText(),textFieldMM.getText(),textFieldSS.getText());
        if(ms<=0){
            showError("正しい時間を入力してください(hh>=0,mm>=0,ss<60");
            return;
        }
        remainingMillis=ms;
        targetEndNanos=System.nanoTime()+remainingMillis*1000000L;

        lblTimeLeft.setText(formatHMS(remainingMillis));
        createOrResetTimeLine();
        timeline.play();
        tick();

        state=State.RUNNING;
        updateButtons();
    }

    private void pauseOrResume(){
        switch(state){
            case RUNNING -> {
                timeline.pause();
                remainingMillis=Math.max(0,(targetEndNanos-System.nanoTime())/1000000L);
                state=State.PAUSED;
            }
            case PAUSED -> {
                if(remainingMillis>0){
                    targetEndNanos=System.nanoTime()+remainingMillis*1000000L;
                    timeline.play();
                    state=State.RUNNING;
                    tick();
                }
            }
            default -> {
                //IDLE/FINISHED 都不動作
            }
        }
        updateButtons();
    }
    private void cancelAll(){
        if(timeline!=null){
            timeline.stop();
        }
        remainingMillis=0;
        lblTimeLeft.setText("00:00:00");
        textFieldHH.clear();
        textFieldMM.clear();
        textFieldSS.clear();

        state=State.IDLE;
        updateButtons();
    }
    private void updateButtons(){
        boolean running=(state==State.RUNNING);
        boolean paused=(state==State.PAUSED);
        boolean hasTime=remainingMillis>0;

        //START:在IDLE或FINISHED可按；正在跑/暫停時通常禁用(避免破壞流程)
        btnStart.setDisable(running||paused);

        //PAUSE(或RESUME):
        btnPause.setDisable(!hasTime||state==State.IDLE||state==State.FINISHED);
        btnPause.setText(paused?"RESUME":"PAUSE");

        //CANCEL:只有正在跑或暫停才需要
        btnCancel.setDisable(!(running||paused));

        //輸入欄位:跑起來時不可編輯，避免邊跑邊改
        boolean editable=!running;
        textFieldHH.setEditable(editable);
        textFieldMM.setEditable(editable);
        textFieldSS.setEditable(editable);
    }
    private void showError(String msg){
        Alert alert=new Alert(AlertType.ERROR);
        alert.setTitle("エラー");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

}
