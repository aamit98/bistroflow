package ServiceLayer;

public class Response {
    public final Object returnValue;
    public final String errorMsg;
    public Response(String errorMsg,Object returnValue){
        this.returnValue = returnValue;
        this.errorMsg = errorMsg;
    }
    public Object getReturnValue(){
        return this.returnValue;
    }
    public String getErrorMsg(){
        return this.errorMsg;
    }
    public boolean errorOccurred() { return this.errorMsg!=null;}
}
