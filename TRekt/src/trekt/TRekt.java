/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package trekt;

/**
 *
 * @author Silver
 */
public class TRekt {

        double battlefieldWidth;
    double battlefieldHeight;
    double energiaEnemigo;
    double FUERZA_DE_BALA = 3;

    // TRekt usa la filosofia wave pattern para esquivar, para ello recoge
    // informacion de cuando el enemigo ha disparado. Con esta informacion
    // es posible predecir y simular la posicion de una bala y tenerla en cuenta
    // a la hora de elegir el siguiente movimiento
    private class DisparoEnemigo{
        Point2D.Double origen;
        
        double momento;
        double velocidadBala;
        double angulo;
        
        double velocidadXNuestra;
    }
    ArrayList<Robotijo.DisparoEnemigo> disparosEnemigos = new ArrayList<Robotijo.DisparoEnemigo>();
    
    // ############################################################################################# //
    // ########################## FUNCIONES CUSTOM (no de AdvancedRobot) ########################### //
    // ############################################################################################# //

    public void guardarDisparoEnemigo(ScannedRobotEvent e, Double energiaDisparo){
        double anguloEnemigo = getAnguloEnemigo(e);
        DisparoEnemigo mov = new DisparoEnemigo();
        
        // Guardamos la posicion del enemigo cuando ha disparado usando nuestra
        // posicion, la distancia y el angulo en el que se encuentra
        mov.origen = proyectarPuntoVector(new Point2D.Double(getX(), getY()), e.getDistance(), anguloEnemigo);
        
        // Guardamos el momento en el timepo en el que se disparo para usarlo en la prediccion en un futuro
        mov.momento = getTime();
        
        // Guardamos la velocidad de la bala
        // La formula para la velocidad de la bala ha sido conseguida de 
        // https://robowiki.net/wiki/Robocode/FAQ -> "How fast does a bullet travel?"
        mov.velocidadBala = 20-(3*energiaDisparo);
        
        // El angulo del enemigo a nosotros es el nuestro con un flip en el eje x
        // es a decir, nuestro angulo + pi
        mov.angulo = Utils.normalRelativeAngle(anguloEnemigo+Math.PI);
        
        // Y para acabar, guardamos la velocidad lateral que tenemos en el momento del disparo enemigo.
        // Esto es para tener en cuenta la posibilidad de que el enemigo puede estimar nuestra posicion
        // y disparar en ese punto.
        double velocidadNuestra = getVelocity();
        // Calculamos nuestra velocidad desde la perspectiva del enemigo, haciendo un triangulo con nuestra
        // orientacion y el angulo des de el que nos ve el otro robot. Con esto nos podemos adelantar a la
        // prediccion del enemigo
        double desplX = velocidadNuestra*Math.sin(getHeadingRadians()-mov.angulo);
        double velX = desplX/mov.velocidadBala;
        mov.velocidadXNuestra = velX;
        
        disparosEnemigos.add(mov);
    }
    // Detectemamos el angulo respeto a nosotros en el que esta el enemigo.
    public Double getAnguloEnemigo(ScannedRobotEvent e){
        // Este angulo es el angulo en el que ha sido escaneado e.getBearingRadians()
        // mas el angulo de nuestra orientacion para tener en cuenta el offset que generamos sobre la deteccion.
        return e.getBearingRadians()+getHeadingRadians();
    }
    
    // Calcular coordenada de un punto dado un origen, una distancia y un angulo
    public Point2D.Double proyectarPuntoVector(Point2D.Double origen, double dist, double angulo){
        double newx = origen.x + dist*Math.sin(angulo);
        double newy = origen.y + dist*Math.cos(angulo);
        return new Point2D.Double(newx, newy);
    }
    
    // Mirar si la posicion esta dentro de los margenes del campo
    public Boolean evitaBordes(Point2D.Double posicion){
        if(50 < posicion.x && posicion.x < battlefieldWidth-100)
            if(50 < posicion.y && posicion.y < battlefieldHeight-100)
                return true;
        return false;
    }
    public void elegirDireccionMovimientoo(Point2D.Double enemyLocation){
        System.out.println(Math.PI/16D);
		DisparoEnemigo w;
		//This for loop rates each angle individually
		double bestRating=Double.POSITIVE_INFINITY;
		for(double moveAngle=0;moveAngle<Math.PI*2;moveAngle+=Math.PI/16D){
			double rating=0;
			
			//Movepoint is position we would be at if we were to move one robot-length in the given direction. 
			Point2D.Double movePoint=proyectarPuntoVector(new Point2D.Double(getX(),getY()),36,moveAngle);
			
			/*
			 * This loop will iterate through each wave and add a risk for the simulated bullets on each one
			 * to the total risk for this angle.
			 */
			for(int i=0;i<disparosEnemigos.size();i++){
				w=disparosEnemigos.get(i);
				
				//This part will remove waves that have passed our robot, so we no longer keep taking into account old ones
				if(new Point2D.Double(getX(),getY()).distance(w.origen)<(getTime()-w.momento)*w.velocidadBala+w.velocidadBala){
					disparosEnemigos.remove(w);
				}
				else{
					/*
					 * This adds two risks for each wave: one based on the distance from where a head-on targeting
					 * bullet would be, and one for where a linear targeting bullet would be.
					 */
					rating+=1D/Math.pow(movePoint.distance(proyectarPuntoVector(w.origen,movePoint.distance(w.origen),w.angulo)),2);
					rating+=1D/Math.pow(movePoint.distance(proyectarPuntoVector(w.origen,movePoint.distance(w.origen),w.angulo+w.velocidadXNuestra)),2);
				}
			}
			//This adds a risk associated with being to close to the other robot if there are no waves.
			if(disparosEnemigos.size()==0){
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
    /*
    */    

    // El movimiento del bot esta basado en la estrategia de wave pattern
    public void elegirDireccionMovimiento(Point2D.Double posicionEnemigo){
        // Para decidir cual es el mejor movimiento, mantendremos una puntuacion de cada movimiento
        // y sumaremos puntos en funcion de como los diferentes parametros se ajusten a la funcion
        // 1/(x^2), la cual nos permite conseguir valores muy altos para variables cercanas al 0
        // haciendo que consigan mucha puntuacion.
        // La segunda parte es que los parametros que analizaremos estan mas cercanos al 0 cuanto
        // mas peligrosos son
        // Elegiremos el movimiento con menor puntuacion pues sera el que presente menos riesgo
        DisparoEnemigo prediccion; // Declaramos la prediccion aqui para no reservar memoria cada vez que hacemos get de disparosEnemigos
        // Al primer movimiento le damos puntuacion infinita para que la primera sugerencia valida cumpla que es mejor (tiene menos puntuacion)
        double mejorPuntuacion = Double.POSITIVE_INFINITY;
        double decimaParteVuelta = (2*Math.PI)/36D; // Comprobaremos todos los angulos de 10 en 10 hasta 360
        for(double moveAngle=0; moveAngle < (2*Math.PI); moveAngle += decimaParteVuelta){
            double puntuacion=0;
            
            // Proyectamos la posicion a la que queremos ir usando la filosofia de wave moving en que
            // hacemos un zig-zag suave pero que previene movimientos lineares y nos permite
            // definir movimientos que esquivarian las simulaciones de las balas que generaremos
            Point2D.Double posSiguiente = proyectarPuntoVector(new Point2D.Double(getX(),getY()), 36, moveAngle);
            
            for (int i = 0; i < disparosEnemigos.size(); i++) {
                prediccion = disparosEnemigos.get(i);
                
                // Comprobar si la prediccion ya ha superado a nuestro robot,
                // en cuyo caso la eliminamos de nuestras prediccions
                double distanciaBalaNosotros = new Point2D.Double(getX(), getY()).distance(prediccion.origen);
                double tiempoBalaAire = getTime()-prediccion.momento;
                // Sumamos la velocidad una vez extra porque estamos simulando donde estara cuando nos movamos
                double distanciaBalaRecorrida = (tiempoBalaAire * prediccion.velocidadBala) + prediccion.velocidadBala;
                if(distanciaBalaNosotros < distanciaBalaRecorrida){
                    disparosEnemigos.remove(prediccion);
                } else {
                    // Mejoramos la puntuacion del movimiento basandonos en dos predicciones
                    // 1. Si nos han apuntado de manera directa y sin prevenir nuestro movimiento
                    Point2D.Double posBalaApuntadosDirectamente = proyectarPuntoVector(prediccion.origen, posSiguiente.distance(prediccion.origen), prediccion.angulo);
                    double distanciaApuntadosDirectamente = posSiguiente.distance(posBalaApuntadosDirectamente);
                    // Usamos la filosofia de la funcion 1/(x^2), si hay poca distancia de nosotros a la prediccion es importante que posSiguiente
                    // gane preferencia y lo esquivemos
                    puntuacion += 1D/Math.pow(distanciaApuntadosDirectamente, 2);
                    
                    // 2. Si han apuntado prediciendo donde ibamos a estar
                    // El calculo es extremadamente similar pero este tiene en cuenta nuestra velocidad lateral en el momento en el que detectamos el disparo
                    Point2D.Double posBalaApuntadosPrediccion = proyectarPuntoVector(prediccion.origen, posSiguiente.distance(prediccion.origen), prediccion.angulo + prediccion.velocidadXNuestra);
                    double distanciaApuntadosPrediccion = posSiguiente.distance(posBalaApuntadosPrediccion);
                    puntuacion += 1D/Math.pow(distanciaApuntadosPrediccion, 2);                    
                }
            }

            // Si no tenemos disparosEnemigos, calculamos la distancia al enemigo y usamos la filosofia de la funcion 1/(x^2)
            // para determinar la urgencia de apartarse. Esto nos permite apartarnos de rammers
            if(disparosEnemigos.size() == 0){
                double considerando = posSiguiente.distance(posicionEnemigo);
                puntuacion = 1D/Math.pow(considerando, 2);
            }
                        
            if(puntuacion < mejorPuntuacion ){
                if(evitaBordes(posSiguiente)){
                    mejorPuntuacion = puntuacion;
                    int direccion;
                    // Si la diferencia entre nuestro siguiente angulo de movimiento y la direccion en la que estamos es 
                    // superior a 90 grados hay que hacer un cambio de sentido
                    if(Math.abs(moveAngle-getHeadingRadians()) < (Math.PI/2))
                        direccion = 1;
                    else
                        direccion = -1;
                    // Preparamos movimiento forward en la direccion que nos permite girar hacia nuestro punto
                    setAhead(1000*direccion);
                    
                    // Si hay un cambio de sentido, hay que corregir el movimiento para girar a medida que avanzamos recto
                    double corregir;
                    if(direccion == -1)
                        corregir = Math.PI; // Sumar 90 grados a nuestro giro para poder hacer un cambio de sentido
                    else
                        corregir = 0;
                    // Con la correccion, el nuevo angulo sera el angulo aceptado de movimiento + correcion de 0 o 
                    // 90 grados - compensar por la rotacion actual de nuestro robot
                    double nuevoAngulo = moveAngle + corregir - getHeadingRadians();
                    setTurnRightRadians(Utils.normalRelativeAngle(nuevoAngulo));

                }
            } 
        }
    }
    
    // ############################################################################################# //
    // ################################ OVERWRITES DE LA CLASE ROBOT ############################### //
    // ############################################################################################# //
    
    // La logica principal de este bot se ejecuta en el evento onScannedRobot
    // Usamos run para hacer el setup de las opciones que necesitamos
    public void run(){
        energiaEnemigo = 100;
        battlefieldWidth = getBattleFieldWidth();
        battlefieldHeight = getBattleFieldHeight();
        
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);
        
        while(true){
            // El radar hace lock-on en el enemigo y compensa los grados para poder perseguir su movimiento (en onScannedRobot).
            // La funcion de girar infinitamente en el sentido de las agujas del reloj ha de activarse SOLO cuando no hay un
            // movimiento configurado, es a decir, no estamos haciendo ajustes para perseguir el robot enemigo.
            // Es por eso que si a nuestro radar no le queda giro restante de ajuste por hacer se cumple la condicion
            // y encendemos el modo girar infinitamente hasta volver a encontrar otro bot.
            if(getRadarTurnRemaining()==0){
                setTurnRadarRightRadians(Double.POSITIVE_INFINITY);            
            }
            execute();
        }
    }
    
    public void onScannedRobot(ScannedRobotEvent e){
        // ****************************** Analisis del robot detectado ***************************** //
        double anguloEnemigo = getAnguloEnemigo(e);

        // Detectar si el enemigo ha disparado usando su energia como referencia
        double energiaGastada = energiaEnemigo - e.getEnergy();
        energiaEnemigo = e.getEnergy(); // Actualizamos su energia actual
        // Si el energiaGastada se corresponde con la energia gastada por un disparo, lo guardamos
        if(0.1 <= energiaGastada && energiaGastada <= 3){
            guardarDisparoEnemigo(e, energiaGastada);
        }
        
        // ****************************** Preparar moviemiento ***************************** //
        // Ahora que hemos detectado si el enemigo ha disparado, decidimos a donde movernos
        elegirDireccionMovimiento(proyectarPuntoVector(new Point2D.Double(getX(), getY()), e.getDistance(), anguloEnemigo));
        // ****************************** Apuntar, disparar, ajustar radar ***************************** //
        // Apuntar a su posicion actual
        setTurnGunRightRadians(Utils.normalRelativeAngle(anguloEnemigo-getGunHeadingRadians()));
        // Orden de disparar cuando el arma esta preparada
        // if(getGunHeat() == 0) 
        //    setFire(2);
        // Tracking del enemigo
        // TODO: Explicar formulas
        setTurnRadarRightRadians(Utils.normalRelativeAngle(anguloEnemigo - getRadarHeadingRadians()) * 2);
        
    }
    
    // Cuando hacemos un hit actualizamos la vida estimada de nuestro enemigo
    public void onBulletHit(BulletHitEvent e){
        energiaEnemigo -= FUERZA_DE_BALA;
    }
}
