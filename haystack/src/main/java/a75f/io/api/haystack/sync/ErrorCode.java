package a75f.io.api.haystack.sync;

public class ErrorCode {
    private int code;
    private String description;

    public ErrorCode() { }

    public int getCode() {
        return code;
    }
    public void setCode(int code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "ErrorCode{" +
                "code=" + code +
                ", description='" + description + '\'' +
                '}';
    }
}
