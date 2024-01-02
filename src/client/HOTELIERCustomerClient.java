package client;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import entities.Hotel;

import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.List;

public class HOTELIERCustomerClient {
    public void jsonizza() {
        try (FileReader reader = new FileReader("Hotels.json")) {
            Type listType = new TypeToken<ArrayList<Hotel>>(){}.getType();

            ArrayList<Hotel> hotels = (new Gson()).fromJson(reader, listType);
            hotels.forEach(h -> System.out.println(h));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
