public class Client {
    private final int id;
    private String name;
    private String phone;

    public Client (int id, String name, String phone){
        this.id = id;
        this.name = name;
        this.phone = phone;
    }

    public int getId(){
        return id;
    }

    public String getName(){
        return name;
    }

    public String getPhone(){
        return phone;
    }

    public void setName(String name){
        this.name = name;

    }

    public void setPhone(String phone){
        this.phone = phone;
    }

    public String toString() {
   return "Id : " + this.id +
	  " , Nom :  " + this.name +
	  ", Téléphone : " + this.phone;
    }

}
