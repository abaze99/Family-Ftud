import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.nio.file.Paths;
import java.util.ArrayList;

/**
 * The graphical view for the game and starting point of the program
 */
public class GameGUI extends Application{
	private Polls polls; //holds all of the questions, which each hold their own answers
	private MediaPlayer audio; //is what plays the audio
    ArrayList<AnswerTile> answerTiles; //holds all of the ties in the center, ordered by rank

	private int leftTeam, rightTeam = 0; //used for keeping track of team scores
	private Text leftPoints, currentPointsText, rightPoints;
	private int selectedTeam; //used to signify if the left(-1) or right(1) team is selected, or neither(0)
	private int currentQuestion = -1; //it is incremented to 1 before being used
	private int multiplier = 1;
	private int currentPoints;
	private int numWrong = 0;
	private HBox strikes;
	private StackPane window;

	Rectangle2D screen; //used for increased readability when referencing the screen size

    public void playAudio(String filename){
        if(audio != null) audio.stop();
        Media audioFile = new Media(Paths.get("src/resources/" + filename).toUri().toString());
        audio = new MediaPlayer(audioFile);
        audio.play();
    }

	public void styleText(Text text, double size){
		text.setFont(Font.font("Calibri", FontWeight.BLACK, size));
		text.setStyle("-fx-fill: white; -fx-stroke: black; -fx-stroke-width: " + size/30 + "px");
	}

    public void setImageAsBackground( Region region, String image, double width, double height ){
        BackgroundImage bi = new BackgroundImage(
                new Image("resources\\" + image, width, height, false, true),
                BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.CENTER, BackgroundSize.DEFAULT);
        region.setBackground( new Background(bi) );
    }

    /** Highlights and selects the team given by -1(left), 0(deselect), and 1(right) */
    private void selectTeam(int team){
        if(team == -1){
            selectedTeam = -1;
            setImageAsBackground(window, "left team selected.png", screen.getWidth(), screen.getHeight());
        }else if(team == 0){
            selectedTeam = 0;
            setImageAsBackground(window, "background.png", screen.getWidth(), screen.getHeight());
        }else  if(team == 1){
            selectedTeam = 1;
            setImageAsBackground(window, "right team selected.png", screen.getWidth(), screen.getHeight());
        }
    }

	private void setupQuestion(int i){
        numWrong = 0;
        currentPoints = 0;
        currentPointsText.setText("0");
        selectTeam(0);
        for(AnswerTile tile: answerTiles)
            tile.clear();

        Question q = new Question("Will be replaced by the actual question");
        if(i == 0){ //start from the beginning
            currentQuestion = 0;
            q = polls.questions.get(0);
        }else if(i == -1){ //go backwards one question
            if(currentQuestion > 0){
                q = polls.questions.get(--currentQuestion);
            }
        }else if (i == 1){ //go forwards to the next question
            if(currentQuestion < polls.questions.size()-1){
                q = polls.questions.get(++currentQuestion);
            }
        }

        for(int j=0; j<q.answers.size(); j++)
	        answerTiles.get(j).setAnswer(q.answers.get(j));
    }

    private void wrongAnswer(){
	    ++numWrong;

	    //Make the strike images to appear
	    ArrayList<ImageView> strikemarks = new ArrayList<>();
	    for(int i=0; i<numWrong; i++)
	        strikemarks.add( new ImageView(new Image("resources\\strike.png")) );


	    //Make the transitions for the strikes to appear and disappear with
        FadeTransition disappear = new FadeTransition(Duration.millis(50), strikes);
        disappear.setFromValue(1);
        disappear.setToValue(0);
        disappear.setCycleCount(1);
        disappear.setOnFinished(e -> strikes.getChildren().clear());

        FadeTransition appear = new FadeTransition(Duration.millis(50), strikes);
        appear.setFromValue(0);
        appear.setToValue(1);
        appear.setCycleCount(1);
        appear.setOnFinished(e ->{
            Platform.runLater(() -> {
                try{
                    Thread.sleep(1000); //done in a separate thread to not halt user input
                }catch(Exception exc){exc.printStackTrace();}
                disappear.play();
            });
        });


        //Style the strikes and add them to the screen
	    for(ImageView img : strikemarks){
	        img.setPreserveRatio(true);
	        img.setFitWidth(screen.getWidth()/5);
            strikes.getChildren().addAll(img);
        }


        //Actually play the animation
        playAudio("strike.mp3");
        appear.play();
    }

    public void scoreAnswer(int answerValue){
	    //todo - animate the currentPoint value increasing
        currentPoints += answerValue * multiplier;
        currentPointsText.setText(Integer.toString(currentPoints));
    }

    private void scoreQuestion(){
        //todo - animate the currentPoint value increasing
        if(selectedTeam == -1){
	        leftTeam += currentPoints;
	        leftPoints.setText(Integer.toString(leftTeam));
            scoreAnswer(-currentPoints); //to reset the current unallocated points
        }else if(selectedTeam == 1){
	        rightTeam += currentPoints;
	        rightPoints.setText(Integer.toString(rightTeam));
            scoreAnswer(-currentPoints); //inside braces so it only triggers if a team is selected
        }
    }

	@Override
	public void init(){
		polls = new Polls("questions.txt");
		screen = Screen.getPrimary().getVisualBounds();
	}

	@Override
	public void start(Stage stage){
	    //Setup the overall stage and the top layer for strike animations
		BorderPane game = new BorderPane();
        strikes = new HBox();
        strikes.setAlignment(Pos.CENTER);
        window = new StackPane(game, strikes);
		Scene scene = new Scene(window);


        //Setup the background of the program
        Rectangle2D screen =Screen.getPrimary().getBounds();
        setImageAsBackground(window, "background.png", screen.getWidth(), screen.getHeight());


		//Areas for the team names, scores, and current question value
		BorderPane top = new BorderPane();

		VBox leftFamily = new VBox();
		leftFamily.setAlignment(Pos.CENTER);
		Text leftName = new Text("Hooffields");
        styleText(leftName, screen.getHeight()/10.55);
		leftPoints = new Text("0");
		styleText(leftPoints, screen.getHeight()/5.63);
		leftFamily.getChildren().addAll(leftName, leftPoints);
		top.setLeft(leftFamily);
		leftFamily.setSpacing(screen.getHeight()/100);
		BorderPane.setMargin(leftFamily, new Insets(screen.getHeight()/100, 0, 0, screen.getWidth()/28));

		currentPointsText = new Text("0");
		styleText(currentPointsText, screen.getHeight()/4.69);
		top.setCenter(currentPointsText);
		BorderPane.setMargin(currentPointsText, new Insets(screen.getHeight()/15, screen.getWidth()/30, 0, 0));

		VBox rightFamily = new VBox();
		rightFamily.setAlignment(Pos.CENTER);
		Text rightName = new Text("McColts");
        styleText(rightName, screen.getHeight()/10.55);
		rightPoints = new Text("0");
		styleText(rightPoints, screen.getHeight()/5.63);
		rightFamily.getChildren().addAll(rightName, rightPoints);
		top.setRight(rightFamily);
		rightFamily.setSpacing(screen.getHeight()/100);
		BorderPane.setMargin(rightFamily, new Insets(screen.getHeight()/100, screen.getWidth()/17, 0, 0));

		game.setTop(top);


		//The area containing the actual answers
		answerTiles = new ArrayList<>();

		VBox leftAnswers = new VBox();
		for(int i=1; i<6; i++)
            leftAnswers.getChildren().add(new AnswerTile(this, i));
		leftAnswers.setSpacing(screen.getHeight()/106.6);

		VBox rightAnswers = new VBox();
        for(int i=6; i<11; i++)
            rightAnswers.getChildren().add(new AnswerTile(this, i));
        rightAnswers.setSpacing(screen.getHeight()/106.6);

        HBox answers = new HBox(leftAnswers, rightAnswers);
        answers.setSpacing(screen.getWidth()/150);
		game.setCenter(answers);
		BorderPane.setMargin(answers, new Insets(screen.getHeight()/21, 0, 0, screen.getWidth()/9.01));

		setupQuestion(0);


		//Handles user input with the program
		scene.addEventHandler(KeyEvent.KEY_PRESSED, (key) -> {
			//todo - finish processing keyboard input here
            switch(key.getCode().getName()){
				case "1": answerTiles.get(0).reveal(); break;
				case "2": answerTiles.get(1).reveal(); break;
				case "3": answerTiles.get(2).reveal(); break;
				case "4": answerTiles.get(3).reveal(); break;
				case "5": answerTiles.get(4).reveal(); break;
				case "6": answerTiles.get(5).reveal(); break;
				case "7": answerTiles.get(6).reveal(); break;
				case "8": answerTiles.get(7).reveal(); break;
				case "9": answerTiles.get(8).reveal(); break;
				case "0": answerTiles.get(9).reveal(); break;
/** restart */  case "R": setupQuestion(0); break;
/** back */     case "B": setupQuestion(-1); break;
/** next */     case "N": setupQuestion(1); break;
/** theme */    case "T": playAudio("theme.mp3"); break;
/** strike */	case "X": wrongAnswer(); break;
/** stop */		case "S": if(audio != null) audio.stop(); break;
				case "Left": selectTeam(-1); break;
				case "Right": selectTeam(1); break;
				case "Up": multiplier++; break;
				case "Down": if(multiplier > 1) multiplier--; break;
                case "Space": scoreQuestion();
                case "Backspace": break; //todo - undo
				case "Enter": break; //todo - redoes if an action was just undone, else goes to fast money
			}
		});


		stage.setScene(scene);
		stage.setTitle("Family Feud");
		stage.setFullScreen(true);
		stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH); //removes the esc hint and keeps it fullscreen
        //todo - take a screenshot of completed look and use it as an icon
		stage.show();
	}

	public static void main(String[] args){ Application.launch(args); }
}
