/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package porra;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Classe principal de la porra.
 *  Aquí ens connectem a la base de dades i l'usuari disposa d'un menú de tres
 *  opcions per escollir què vol fer:
 * 
 *  1. Fer una aposta:
 *      Tant per aquesta com per la segona opció (veure si has guanyat) es
 *      requereix que l'usuari estigui registrat. Així que se li demana
 *      el dni. Si el dni es troba a la base de dades, se li dóna un missatge
 *      de benvinguda i pot començar a fer l'aposta. En cas que no es trobés
 *      registrat, se li demana el nom i el dni per registrar-lo.
 *      A l'hora de fer l'aposta, se li demanen els gols del local i els gols
 *      del visitant. Abans de portar-la a terme, es comprova si aquest mateix
 *      usuari ja havia fet aquesta mateixa aposta. En cas que si, se li avisa
 *      i l'aposta es cancel·la. En cas que no, se li avisa de que ha estat
 *      feta amb èxit.
 * 
 *  2. Veure si has guanyat:
 *      L'usuari ja està registrat i disposem del seu dni, així que comprovem
 *      els resultats de l'aposta i mirem si coincideixen amb alguna aposta
 *      que l'usuari havia fet. En cas que si, per cada aposta encertada, se
 *      li donen 5$. En cas que no, surt un missatge dient que és pèssim com a 
 *      visionari.
 * 
 *  3. Veure quantes apostes s'han fet i quants usuaris hi ha registrats:
 *      Es fa un count(id_aposta) per quantes apostes hi ha i un 
 *      count(distinct dni_usuari) per saber quants usuaris registrats.
 * 
 * @author clara
 */
public class Porra {

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        Connection conn = null;
        PreparedStatement pre = null;
        PreparedStatement pre2 = null;
        ResultSet rs = null;
        ResultSet rs2 = null;

        /* Connectar a la BBDD */
        String urlJDBC = "jdbc:sqlite:porra.sqlite";
        try {
            conn = Connectin.getConnexio(urlJDBC);
            System.out.println("Connectat");
        } catch (SQLException e) {
            System.err.println("Can't connect, buddy");
        } catch (Exception ex) {
            Logger.getLogger(Porra.class.getName()).log(Level.SEVERE, null, ex);
        }

        /* Menú d'opcions */
        System.out.println("Benvingut a la porra de futbol.");

        System.out.println("Què vols fer? [1-3]");
        System.out.println("1. Fer una aposta");
        System.out.println("2. Veure si he guanyat");
        System.out.println("3. Veure quantes apostes i usuaris tenim");
        int opcio = sc.nextInt();

        if (opcio == 1 || opcio == 2) { // Tant sigui la primera com la segona opció, s'haurà de comprovar abans que l'usuari estigui registrat
            /* Comprovar que l'usuari està registrat */
            System.out.println("Entra amb el teu DNI");
            String dni = sc.next();

            boolean registrat = false;

            String sql = "SELECT nom FROM registrats WHERE dni = ?";
            try {
                pre = conn.prepareStatement(sql);
                pre.setString(1, dni);

                rs = pre.executeQuery();
                while (rs.next()) {
                    System.out.println("Welcome, " + rs.getString("nom") + " ");
                    registrat = true;
                }
            } catch (SQLException e) {
                System.out.println(e);
            }

            if (!registrat) { // Si no ho estava, el registrem
                System.out.println("Sorry mate! No tenim aquest DNI registrat");
                System.out.println("Però et pots donar d'alta amb el teu DNI i el teu nom");
                System.out.println("DNI?");
                dni = sc.next();
                System.out.println("Nom?");
                String nom = sc.next();

                try {
                    sql = "INSERT INTO registrats VALUES (?, ?)";
                    pre = conn.prepareStatement(sql);
                    pre.setString(1, dni);
                    pre.setString(2, nom);
                    pre.executeUpdate();
                    System.out.println("Ja estàs registrat!");
                } catch (SQLException e) {
                    System.out.println("Problemes amb l'SQL");
                    System.out.println(e);
                }
            }

            if (opcio == 1) { /* Fer l'aposta */
                System.out.println("-------------------------------------------");
                System.out.println("LA TEVA APOSTA: ");
                System.out.println("Gols LOCAL: ");
                int local = sc.nextInt();
                System.out.println("Gols VISITANT: ");
                int visitant = sc.nextInt();

                // Comprovar si aquest mateix usuari ja havia fet aquesta mateixa aposta
                boolean existent = false;
                sql = "SELECT gols_local, gols_visitant FROM aposta WHERE dni_usuari = ?";

                try {
                    pre = conn.prepareStatement(sql);
                    pre.setString(1, dni);
                    rs = pre.executeQuery();
                    while (rs.next()) {
                        int db_local = rs.getInt("gols_local");
                        int db_visitant = rs.getInt("gols_visitant");

                        if (db_local == local && db_visitant == visitant) {
                            existent = true;
                        }
                    }
                } catch (SQLException e) {
                    System.out.println(e);
                }

                if (!existent) { // Si aquesta aposta no existeix
                    try {
                        sql = "INSERT INTO aposta (dni_usuari, gols_local, gols_visitant) VALUES (?, ?, ?)";
                        pre = conn.prepareStatement(sql);
                        pre.setString(1, dni);
                        pre.setInt(2, local);
                        pre.setInt(3, visitant);
                        pre.executeUpdate();
                        System.out.println("Aposta feta!");
                    } catch (SQLException e) {
                        System.out.println("Problemes amb l'SQL");
                        System.out.println(e);
                    }
                } else { // Si existeix
                    System.out.println("Hey, buddy, ja has fet aquesta mateixa aposta!");
                }
            } else { /* Opció 2: Veure si has guanyat*/
                System.out.println("-------------------------------------------");
                sql = "SELECT gols_local, gols_visitant FROM aposta WHERE dni_usuari = ?"; // Agafem totes les apostes fetes d'aquest usuari
                String sql2 = "SELECT gols_local, gols_visitant FROM resultats"; // I tots els resultats
                
                try {
                    pre = conn.prepareStatement(sql);
                    pre2 = conn.prepareStatement(sql2);
                    pre.setString(1, dni);
                    rs = pre.executeQuery();
                    rs2 = pre2.executeQuery();

                    int count = 0;
                    boolean won = false;

                    while (rs.next() && rs2.next()) {
                        int local = rs.getInt("gols_local");
                        int visitant = rs.getInt("gols_visitant");
                        int res_local = rs2.getInt("gols_local");
                        int res_visitant = rs2.getInt("gols_visitant");

                        if (res_local == local && res_visitant == visitant) { // Per aposta que coincideixi, es sumen 5$
                            count += 5;
                            won = true;
                        }

                    }
                    if (won) {
                        System.out.println("EY! Has guanyat " + count + "$");
                    } else {
                        System.out.println("Sorry mate! Ets un mal visionari");
                    }
                } catch (SQLException e) {
                    System.out.println(e);
                }
            }
        } else if (opcio == 3) { /* Opció 3: Veure quantes apostes s'han fet i quants usuaris hi ha registrats */
            System.out.println("-------------------------------------------");
            String sql = "select count(id_aposta), count(distinct dni_usuari) from aposta";
            try {
                pre = conn.prepareStatement(sql);
                rs = pre.executeQuery();
                while (rs.next()) {
                    System.out.println("S'han fet un total de " + rs.getInt("count(id_aposta)") + " apostes amb " + rs.getInt("count(distinct dni_usuari)") + " usuaris registrats");
                }
            } catch (SQLException e) {
                System.out.println(e);
            }
        } else {
            System.out.println("Excuse me: [1-3]");
        }
        
        /* Tancar connexió */
        try {
            pre.close();
            conn.close();
        } catch (SQLException e) {
            System.err.println("Problemes amb tancar la connexió");
        }

    }
}
