package wash.rocket.xor.rocketwash.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class ReservedResult {

    @JsonProperty("status")
    private String status;

    @JsonProperty("data")
    private List<Reservation> data;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<Reservation> getData() {
        return data;
    }

    public void setData(List<Reservation> data) {
        this.data = data;
    }
}