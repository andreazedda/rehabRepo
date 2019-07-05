using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using TechTweaking.Bluetooth;
using System;
using System.IO;

public class Sensor : MonoBehaviour
{

    public GameObject empty;
    public GameObject sphereLeft;
    public GameObject sphereMiddle;
    public GameObject sphereRight;

    private static string fileName;

    private BluetoothDevice device;
    private string residuo = ""; // parte iniziale dato letto da bluetooth
    private List<string> stringhe; // lista stringhe lette da bluetooth 
    private int stringhe_i;

    private Quaternion now;
    private bool readyToMove = false;
    private float offset = 0.01f;

    private bool readyToMoveAcc = false;
    private Vector3 pos = new Vector3(0f, 0f, 0f);

    Gradient gradient;
    GradientColorKey[] colorKey;
    GradientAlphaKey[] alphaKey;

    private bool startStop = false;
    private int[] bersaglio = { 0, 0, 0 };

    void Start()
    {

        BluetoothAdapter.enableBluetooth();
        device = new BluetoothDevice();
        // visualizzo il macAddress dall'app android (pulsante connessione)
        device.MacAddress = "D4:40:F0:B5:51:FE";
        device.UUID = "00001101-0000-1000-8000-00805F9B34FB";
        device.connect(); // fa la connessione bluetooth 
        now = new Quaternion(1, 1, 1, 1); // inizializzazione come da manuale ma non so perché lo inizializzi così
        stringhe = new List<string>(); // inizializzo la lista di stringhe lette dal bluetooth
        stringhe_i = 0; // inizializzo indice di lettura di 'stinghe'

        gradient = new Gradient(); // inizializzo il gradient impostando sotto i colori con la rispettiva scala da 0 a 1

        // Populate the color keys at the relative time 0 and 1 (0 and 100%)
        colorKey = new GradientColorKey[2];
        colorKey[0].color = Color.red;
        colorKey[0].time = 0.0f;
        colorKey[1].color = Color.blue;
        colorKey[1].time = 1.0f;

        // Populate the alpha  keys at relative time 0 and 1  (0 and 100%)
        alphaKey = new GradientAlphaKey[2];
        alphaKey[0].alpha = 1.0f;
        alphaKey[0].time = 0.0f;
        alphaKey[1].alpha = 0.0f;
        alphaKey[1].time = 1.0f;

        gradient.SetKeys(colorKey, alphaKey); // imposta il gradiente con i parametri impostati (vedi parseValuesAcc)


    }

    // Update is called once per frame
    void Update()
    {

        bersaglio[0] = sphereLeft.GetComponent<Renderer>().material.color == Color.blue ? 1 : 0;
        bersaglio[1] = sphereMiddle.GetComponent<Renderer>().material.color == Color.blue ? 1 : 0;
        bersaglio[2] = sphereRight.GetComponent<Renderer>().material.color == Color.blue ? 1 : 0;

        // se stringhe è vuoto leggo i dati dal bluetooth       
        if (stringhe.Count == 0)
        {
            if (device.IsReading)
            {
                byte[] msg = device.read();
                if (msg != null)
                {
                    string content = System.Text.ASCIIEncoding.ASCII.GetString(msg);
                    parseContent(content); // parse content divide la stringa letta (vedi app android) e inserisce in stringhe il risultato 
                }

            }
        }


        if (stringhe_i < stringhe.Count)
        {

            parseValues(stringhe[stringhe_i]);
            moveObj();
            stringhe_i += 1;
        }
        else
        {
            stringhe.Clear();
            stringhe_i = 0;
        }
    }

    void parseContent(string av)
    {
        try
        {
            string[] split = av.Split('#');
            if (split != null && split.Length > 0)
            {
                for (int i = 0; i < split.Length; i++)
                {
                    if (split[i].Length > 0)
                    {
                        if (split[i].StartsWith("$")) // inizia con $?
                        {
                            if (split[i].Substring(split[i].Length - 1) == "$") // l'ultimo carattere è $?
                            {
                                stringhe.Add(split[i].Substring(1, split[i].Length - 2)); // mette nella lista di stringhe
                            }
                            else
                            {
                                residuo = split[i].Substring(1, split[i].Length - 1);
                            }
                        }
                        else
                        {
                            if (split[i].Substring(split[i].Length - 1) == "$")
                            {
                                stringhe.Add(residuo + split[i].Substring(0, split[i].Length - 1)); // concatena a residuo e aggiunge tutto a stringhe
                                residuo = "";
                            }
                            else
                            {
                                residuo = residuo + split[i]; // riporta al caso $ iniziale e non finale
                            }

                        }
                    }
                }
            }
        }
        catch (Exception e)
        {
            Debug.Log("ACC: ERRORE " + av);
        }

    }

    void parseValues(string av)
    {
        try
        {
            string[] split = av.Split(',');
            if (split.Length > 2)
            {
                if (split[0] == "Set" && split[1] == "start" && split.Length == 3) // vedi android per il messaggio di reset
                {
                    int value = int.Parse(split[2]);
                    if (value == 1)
                    {
                        transform.localPosition = new Vector3(0.0f, 0.0f, 0.0f);
                        empty.transform.localEulerAngles = new Vector3(0.0f, 0.0f, 0.0f);
                        sphereLeft.GetComponent<Renderer>().material.color = Color.white;
                        sphereMiddle.GetComponent<Renderer>().material.color = Color.white;
                        sphereRight.GetComponent<Renderer>().material.color = Color.white;
                        startStop = true;
                        startTracking();
                    }
                }

                if (split[0] == "Set" && split[1] == "stop" && split.Length == 3) // vedi android per il messaggio di reset
                {
                    int value = int.Parse(split[2]);
                    if (value == 1)
                    {
                        transform.localPosition = new Vector3(0.0f, 0.0f, 0.0f);
                        empty.transform.localEulerAngles = new Vector3(0.0f, 0.0f, 0.0f);
                        sphereLeft.GetComponent<Renderer>().material.color = Color.white;
                        sphereMiddle.GetComponent<Renderer>().material.color = Color.white;
                        sphereRight.GetComponent<Renderer>().material.color = Color.white;
                        startStop = false;
                    }
                }


                if (split[0] == "Set" && split[1] == "reset" && split.Length == 3) // vedi android per il messaggio di reset
                {
                    int value = int.Parse(split[2]);
                    if (value == 1)
                    {
                        transform.localPosition = new Vector3(0.0f, 0.0f, 0.0f);
                        empty.transform.localEulerAngles = new Vector3(0.0f, 0.0f, 0.0f);
                    }
                }

                if (split[0] == "Set" && split[1] == "hold" && split.Length == 3) // seek bar vedi android 
                {
                    int value = int.Parse(split[2]);
                    float color = value / 100f;
                    GetComponent<Renderer>().material.color = gradient.Evaluate(color);
                }

                if (split[0] == "Gyro" && split[1] == "quat" && split.Length == 6)
                {
                    float x = float.Parse(split[2]);
                    float y = float.Parse(split[4]);
                    float z = float.Parse(split[3]);
                    float w = float.Parse(split[5]);
                    if (!float.IsNaN(x) && !float.IsNaN(y) && !float.IsNaN(z) && !float.IsNaN(w))
                    {
                        if (Math.Abs(x) > offset || Math.Abs(y) > offset || Math.Abs(z) > offset)
                        {
                            now = new Quaternion(-x, -y, -z, w);
                            readyToMove = true;
                        }
                    }
                }

                if (split[0] == "Accelerometer" && split[1] == "values" && split.Length == 6)
                {
                    float x1 = float.Parse(split[2]);
                    float y1 = float.Parse(split[3]);
                    float z1 = float.Parse(split[4]);

                    long timestamp = long.Parse(split[5]); //tempo. vedi android nei dati mandati dall'accelerometro (onSensorChanged)

                    if (!float.IsNaN(x1) && !float.IsNaN(y1) && !float.IsNaN(z1))
                    {
                        pos = new Vector3(x1 * 500f, z1 * 500f, y1 * 500f);
                        readyToMoveAcc = true;
                    }
                }
            }
        }
        catch (System.Exception e)
        {
            Debug.Log("ACC coordinata stringa errata: " + av);
        }
    }

    void moveObj()
    {
        // rotazione con quaternioni
        if (readyToMove)
        {
            Quaternion rot = empty.transform.rotation * now;
            empty.transform.rotation = Quaternion.Slerp(empty.transform.rotation, rot, Time.deltaTime * 60f);
            readyToMove = false;
        }

        if (readyToMoveAcc)
        {
            transform.localPosition = new Vector3(transform.localPosition.x, transform.localPosition.y, pos.z);
            readyToMoveAcc = false;
            writeValues(transform.position, bersaglio);
        }

    }

    void OnDisable()
    {
        if (device != null)
            device.close();
    }


    void startTracking()
    {
        try
        {
            string myPath = Application.persistentDataPath;
            fileName = myPath + "/tracking" + System.DateTime.Now.ToString("yyyy-MM-dd-HH-mm-ss") + ".txt";
            if (File.Exists(fileName))
            {
                return;
            }
            StreamWriter sr = File.CreateText(fileName);
            sr.Close();
        }
        catch (Exception e)
        {
            Debug.Log("ErrorWritingFile" + e.Message);
        }
    }

    void writeValues(Vector3 pos, int[] bersaglio)
    {
        try
        {
            if (fileName != null && File.Exists(fileName))
            {
                StreamWriter w = File.AppendText(fileName);
                w.WriteLine("{0},{1},{2},{3},{4},{5} \r\n", pos.x, pos.y, pos.z, bersaglio[0], bersaglio[1], bersaglio[2]);
                w.Close();
            }
        }
        catch (Exception e)
        {
            Debug.Log("ErrorWritingFile" + e.Message);
        }

    }



}