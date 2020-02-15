import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.io.InputStream;

public class Server 
{
   
    public static void main(String[] args) 
    {
        try 
        {
            
            print("Server Address : " + InetAddress.getLocalHost().getHostAddress() + "\n");
            ServerSocket serverSocket = new ServerSocket(80);
            while (true) 
            {
                final Socket TmpSocket = serverSocket.accept();
                print("Connection accepted\n");
                InputStream inp = TmpSocket.getInputStream();
                while(true)
                {
                    if (inp.available() > 0) {
                        byte[] buff = new byte[inp.available()];
                        inp.read(buff);
                        System.out.println(new String(buff));
                        break;
                    }
                }
                inp.close();
                TmpSocket.close();
            }
        } 
        catch (Exception ex) 
        {
            print("Error : " + ex.getMessage() + "\nRestart server manually\n");
        }
    }

    private static void print(String Message) 
    {
        System.out.print(Message);
    }
}