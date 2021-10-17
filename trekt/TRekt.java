/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package trekt;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import static java.lang.Math.sin;
import robocode.AdvancedRobot;
import robocode.HitByBulletEvent;
import robocode.Robot;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;

public class TRekt extends AdvancedRobot {  
    
    private double predictedX = 0.0;
    private double predictedY = 0.0;
    private double enemyX = 0.0;
    private double enemyY = 0.0;

    
    public void run() {  
        setColors(Color.green, Color.green, Color.black);
        turnLeft(getHeading());
        setTurnRadarRight(Double.POSITIVE_INFINITY);
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);
        
        while(true) {  
            if(getX() < getBattleFieldWidth()/4 || getX() > 3*getBattleFieldWidth()/4 || getY() < getBattleFieldHeight()/4 || getY() > 3*getBattleFieldHeight()/4)  {
                setTurnRight(15);
            }
            setTurnRight(15);
            setAhead(200);
            execute(); 
        }  
    }   

    public void onScannedRobot(ScannedRobotEvent e) { 
        setTurnRadarRight(2.0 * normalizeBearing(getHeading() + e.getBearing() - getRadarHeading()));
        Point2D objective = predictPosition(e.getHeadingRadians(), e.getDistance(), e.getVelocity(), e.getBearingRadians(), 1);
        shoot(objective, e.getBearing(), e.getDistance(), 1);                
    }  
    
    public void onHitByBullet(HitByBulletEvent e) {  
        setTurnRadarRight(Double.POSITIVE_INFINITY);
    }  
        
    @Override
    public void onPaint(Graphics2D g) {
        int r = 250;
        g.setColor(Color.green);
        g.drawOval((int)getX()-r, (int)getY()-r, 2*r, 2*r);
        
        int r2 = 30;
        g.drawOval((int)predictedX-r2, (int)predictedY-r2, 2*r2, 2*r2);
    }
    
    double normalizeBearing(double bearing) {
        while (bearing > 180) bearing -= 360;
        while (bearing < -180) bearing += 360;
        return bearing;
    }

    /*
        Predice la futura posición del tanque rival para el siguiente turno y 
        devuelve un punto con sus coordenadas.
    */
    private Point2D predictPosition(double e_heading, double e_distance, double e_velocity, double e_bearing, double firePower) {
        //bearing es el angulo(en radianes) en el cual se encuentra el tanque 
        //rival, siendo 0 el norte, pi/2 el este, pi el sur y (2/3)*pi el oeste. 
        double bearing = getHeadingRadians() + e_bearing;
        
        //Obtención de las coordenadas del tanque rival a partir de trigonometria.
        enemyX = getX() + e_distance * Math.sin(bearing);
        enemyY = getY() + e_distance * Math.cos(bearing);
        
        //Calcula la velocidad de la bala.
        double bulletVelocity = 20 - firePower * 3;
        
        //Tiempo que tarda la bala en alcanzar el tanque enemigo. No es exacto 
        //porque tomamos la posición escaneada y el tanque se irá moviendo(se supone),
        //pero es un tiempo bastante aproximado al que queremos.
        double time = e_distance / bulletVelocity;
        
        //Distancia que habrá avanzado el tanque enemigo según su orientación.
        double predictedDistance = e_velocity * time;
        
        //Predicción del movimiento del tanque rival a partir de trigonometria.
        predictedX = enemyX + predictedDistance * Math.sin(e_heading);
        predictedY = enemyY + predictedDistance * Math.cos(e_heading);
        
        //Corrige la predicción en caso de que las coordenadas predichas se salgan
        //del rango del mapa.
        if(predictedX > getBattleFieldWidth()) predictedX = getBattleFieldWidth();
        else if(predictedX < 0) predictedX = 0;
        if(predictedY > getBattleFieldHeight()) predictedY = getBattleFieldHeight();
        else if(predictedY < 0) predictedY = 0;
        
        Point2D predictedPos = new Point2D.Double(predictedX, predictedY);
        
        return predictedPos;
    }
    
    /*
    Mueve el cañon del tanque en dirección de las coordenadas del objective y dispara.
    con una potencia de firePower
    */
    private void shoot(Point2D objective, double e_bearing, double e_distance, double firePower) {
        double predictedX = objective.getX();
        double predictedY = objective.getY();
        
        //Angulo que hay entre la posición del tanque rival y su futura
        //posición, tomando como centro la posición de nuestro tanque.
        double predictionAngle = Math.atan2(predictedY - getY(), predictedX - getX()) - Math.atan2(enemyY - getY(), enemyX - getX());
        predictionAngle = normalizeBearing(Math.toDegrees(-predictionAngle));
        
        //Posicionamiento del cañon apuntando a la posición predecida.
        setTurnGunRight(normalizeBearing( Math.toDegrees(getHeadingRadians()) + e_bearing - Math.toDegrees(getGunHeadingRadians())  + predictionAngle ));
        setFire(firePower);
    }
    
    
}
 
