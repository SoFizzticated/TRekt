/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.upc.epsevg.prop;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import robocode.*;
import robocode.util.*;

/**
 *
 * @author Silver
 */
public class TRekt extends AdvancedRobot {
    private double FIRE_POWER=2;
    private double FIRE_SPEED=20-FIRE_POWER*3;
    private double BULLET_DAMAGE=10;
    
    private double enemyEnergy;

    // Onada que ens permet deduir el moviment de l'enemic
    private class MoveWave{
        Point2D.Double origin;
        
        double startTime;
        double speed;
        double angle;
        
        double latVel;
    }
    // TODO: Fill
    private class GunWave{
        Point2D.Double origin;

        double startTime;    
        double speed;
        double absBearing; // TODO: Canviar a angle
        int velSeg;
    }
    
    // Emmagatzemem ones per a calcular la posició de l'enemic
    ArrayList<TRekt.MoveWave> moveWaves = new ArrayList<TRekt.MoveWave>();
    ArrayList<TRekt.GunWave> gunWaves = new ArrayList<TRekt.GunWave>();
    double gunAngles[] = new double[16];
    
    // ############################ FUNCIONES NECESARIAS PARA EL CODIGO ############################ //
    // Detectemamos el angulo respeto a nosotros en el que esta el enemigo.
    public Double getAnguloEnemigo(ScannedRobotEvent e){
        // Este angulo es el angulo en el que ha sido escaneado e.getBearingRadians()
        // mas el angulo de nuestra orientacion para tener en cuenta el offset que generamos sobre la deteccion.
        return e.getBearingRadians()+getHeadingRadians();
    }
    // Calcular coordenada de un punto dado un origen, una distancia y un angulo
    public Point2D.Double proyectarPuntoVector(Point2D.Double origen, double dist, double angulo){
        double newx = origen.x + dist*math.sin(angulo);
        double newy = origen.y + dist*math.sin(angulo);
        return new Point2D.Double(newx, newy);
    }
    public void chooseDirection(Point2D.Double enemyLocation){
        MovementWave w;
        // This for loop rates each angle individually
        double bestRating=Double.POSITIVE_INFINITY;
        for(double moveAngle=0; moveAngle < Math.PI*2; moveAngle += Math.PI/16D){
            double rating=0;
            
            // Calculamos la posicion en la que nos encotrariamos si nos movemos en la direccion guardada
            Point2D.Double movePoint = proyectarPuntoVector(new Point2D.Double(getX(),getY()), 36, moveAngle);
            
            /*
             * This loop will iterate through each wave and add a risk for the simulated bullets on each one
             * to the total risk for this angle.
             */
            for(int i=0;i<moveWaves.size();i++){
                w=moveWaves.get(i);
                
                //This part will remove waves that have passed our robot, so we no longer keep taking into account old ones
                if(new Point2D.Double(getX(),getY()).distance(w.origin)<(getTime()-w.startTime)*w.speed+w.speed){
                    moveWaves.remove(w);
                }
                else{
                    /*
                     * This adds two risks for each wave: one based on the distance from where a head-on targeting
                     * bullet would be, and one for where a linear targeting bullet would be.
                     */
                    rating+=1D/Math.pow(movePoint.distance(proyectarPuntoVector(w.origin,movePoint.distance(w.origin),w.angle)),2);
                    rating+=1D/Math.pow(movePoint.distance(proyectarPuntoVector(w.origin,movePoint.distance(w.origin),w.angle+w.latVel)),2);
                }
            }
            //This adds a risk associated with being to close to the other robot if there are no waves.
            if(moveWaves.size()==0){
                rating=1D/Math.pow(movePoint.distance(enemyLocation),2);
            }
            //This part tells us to go in the direction if it is better than the previous best option and is reachable.
            if(rating<bestRating&&new Rectangle2D.Double(50,50,getBattleFieldWidth()-100,getBattleFieldHeight()-100).contains(movePoint)){
                bestRating=rating;
                /*
                 * These next three lines are a very codesize-efficient way to 
                 * choose the best direction for moving to a point.
                 */
                int pointDir;
                setAhead(1000*(pointDir=(Math.abs(moveAngle-getHeadingRadians())<Math.PI/2?1:-1)));
                setTurnRightRadians(Utils.normalRelativeAngle(moveAngle+(pointDir==-1?Math.PI:0)-getHeadingRadians()));
            }
        }
    }
    public void logMovementWave(ScannedRobotEvent e, double energyChange){
        double absBearing=e.getBearingRadians()+getHeadingRadians();
        MovementWave w=new MovementWave();
        //This is the spot that the enemy was in when they fired.
        w.origin=project(new Point2D.Double(getX(),getY()),e.getDistance(),absBearing);
        //20-3*bulletPower is the formula to find a bullet's speed.
        w.speed=20-3*energyChange;
        //The time at which the bullet was fired.
        w.startTime=getTime();
        //The absolute bearing from the enemy to us can be found by adding Pi to our absolute bearing.
        w.angle=Utils.normalRelativeAngle(absBearing+Math.PI);
        /*
         * Our lateral velocity, used to calculate where a bullet fired with linear targeting would be.
         * Note that the speed has already been factored into the calculation.
         */
        w.latVel=(getVelocity()*Math.sin(getHeadingRadians()-w.angle))/w.speed;
        //This actually adds the wave to the list.
        moveWaves.add(w);
    }

    // ################################ OVERWRITES DE LA CLASE ROBOT ############################### //
    public void run(){
        enemyEnergy = 100;

        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);

        while(true){
            // Girar radar infinito -> parado por onScannedRobot
            setTurnRadarRightRadians(Double.POSITIVE_INFINITY);
            execute();
        }
    }
    
    public void onScannedRobot(ScannedRobotEvent e){
        // ****************************** Analisis del robot detectado ***************************** //
        double absBearing = getAnguloEnemigo();

        // TODO: Change variable names
        // Miramos el nuevo nivel de energia del robot enemigo
        double energyChange = enemyEnergy - e.getEnergy();
        enemyEnergy = e.getEnergy();
        // Si la diferencia está entre 0.1 y 3, el enemigo ha disparado
        if(0.1<=energyChange && energyChange<=3){
            // Buscamos un posible nuevo spot de movimiento
            logMovementWave(e,energyChange);
        }

        // ****************************** Actuar en funcion de los datos ***************************** //
        // Ahora que hemos detectado si el enemigo ha disparado, decidimos a donde movernos
        chooseDirection(proyectarPuntoVector(new Point2D.Double(getX(),getY()), e.getDistance(), absBearing));

        // Comprobar que podemos disparar
        if(getGunHeat()==0){
            // TODO: Implement
            // logFiringWave(e);
        }
        // Mirar si les ones han arribat a l'enemic        
        checkFiringWaves(proyectarPuntoVector(new Point2D.Double(getX(),getY()),e.getDistance(),absBearing));
        // TODO: Fix this mumbo jumbo
        // double gunAngle = 8;
        // gunAngle += e.getVelocity()*Math.sin(e.getHeadingRadians()-absBearing);
        // setTurnGunRightRadians(Utils.normalRelativeAngle(absBearing-getGunHeadingRadians())
        //     +gunAngles[8+(int)(e.getVelocity()*Math.sin(e.getHeadingRadians()-absBearing))]);
        // setFire(FIRE_POWER);
        // TODO: Justify why
        setTurnRadarRightRadians(Utils.normalRelativeAngle(absBearing-getRadarHeadingRadians())*2);
    }

    // Quan fem un impacte restem el mal a la vida estimada de l'enemic
    public void onBulletHit(BulletHitEvent e){
        enemyEnergy -= BULLET_DAMAGE;
    }
}
