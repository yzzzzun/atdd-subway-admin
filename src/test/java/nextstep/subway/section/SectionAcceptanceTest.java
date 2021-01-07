package nextstep.subway.section;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import nextstep.subway.AcceptanceTest;
import nextstep.subway.line.LineAcceptanceTest;
import nextstep.subway.line.dto.LineRequest;
import nextstep.subway.line.dto.LineResponse;
import nextstep.subway.section.dto.SectionRequest;
import nextstep.subway.section.dto.SectionResponse;
import nextstep.subway.station.StationAcceptanceTest;
import nextstep.subway.station.dto.StationRequest;
import nextstep.subway.station.dto.StationResponse;
import nextstep.subway.utils.RequestTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class SectionAcceptanceTest extends AcceptanceTest {
    private static final int INIT_DISTANCE = 30;
    private StationResponse lastUpStation;
    private StationResponse lastDownStation;
    private LineResponse line;

    @BeforeEach
    public void setUp() {
        super.setUp();
        // given
        // 지하철_역_등록_되어있음
        lastUpStation = StationAcceptanceTest.createRequest(new StationRequest("도림천"))
                .as(StationResponse.class);
        lastDownStation = StationAcceptanceTest.createRequest(new StationRequest("신정네거리"))
                .as(StationResponse.class);

        line = LineAcceptanceTest.createRequest(new LineRequest("2호선", "초록색", lastUpStation.getId(), lastDownStation.getId(), INIT_DISTANCE))
                .as(LineResponse.class);
    }

    @DisplayName("상행, 하행 역이 동일한 지하철 구간을 등록한다")
    @Test
    void createDuplicateStation() {
        ExtractableResponse<Response> response = createRequest(line.getId(), new SectionRequest(lastUpStation.getId(), lastUpStation.getId(), 3));
        assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    @DisplayName("이미 등록 된 지하철 구간을 등록한다")
    @Test
    void createDuplicateSection() {
        ExtractableResponse<Response> response = createRequest(line.getId(), new SectionRequest(lastUpStation.getId(), lastDownStation.getId(), 3));
        assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    @DisplayName("노선이 없는 지하철 구간을 등록한다")
    @Test
    void createWithNotExistsLine() {
        ExtractableResponse<Response> response = createRequest(10L, new SectionRequest(lastUpStation.getId(), lastDownStation.getId(), 3));
        assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    @DisplayName("역이 없는 지하철 구간을 등록한다")
    @Test
    void createWithNotExistsStation() {
        ExtractableResponse<Response> response = createRequest(line.getId(), new SectionRequest(lastUpStation.getId(), 10L, 3));
        assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    @DisplayName("구간 사이에 구간 거리가 더 긴 새로운 구간을 등록한다")
    @Test
    void createBetweenOverDistance() {
        StationResponse station = StationAcceptanceTest.createRequest(new StationRequest("양천구청"))
                .as(StationResponse.class);

        ExtractableResponse<Response> upResponse = createRequest(line.getId(), new SectionRequest(lastUpStation.getId(), station.getId(), 50));
        assertThat(upResponse.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());

        ExtractableResponse<Response> downResponse = createRequest(line.getId(), new SectionRequest(station.getId(), lastDownStation.getId(), 50));
        assertThat(downResponse.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    @DisplayName("구간 사이에 새로운 구간을 등록한다(상행기준)")
    @Test
    void createBetween() {
        StationResponse station = StationAcceptanceTest.createRequest(new StationRequest("양천구청"))
                .as(StationResponse.class);

        ExtractableResponse<Response> response = createRequest(line.getId(), new SectionRequest(lastUpStation.getId(), station.getId(), 3));
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());
        ExtractableResponse<Response> selectedResponse = selectRequest(line.getId());
        assertThat(calculateTotalDistance(selectedResponse)).isEqualTo(INIT_DISTANCE);
        List<StationResponse> stations = selectStations(line.getId());
        assertThat(stations).containsExactly(lastUpStation, station, lastDownStation);
    }

    @DisplayName("구간 사이에 새로운 구간을 등록한다(하행기준)")
    @Test
    void createBetween2() {
        StationResponse station = StationAcceptanceTest.createRequest(new StationRequest("양천구청"))
                .as(StationResponse.class);

        ExtractableResponse<Response> response = createRequest(line.getId(), new SectionRequest(station.getId(), lastDownStation.getId(), 3));
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());
        ExtractableResponse<Response> selectedResponse = selectRequest(line.getId());
        assertThat(calculateTotalDistance(selectedResponse)).isEqualTo(INIT_DISTANCE);
        List<StationResponse> stations = selectStations(line.getId());
        assertThat(stations).containsExactly(lastUpStation, station, lastDownStation);
    }


    @DisplayName("새로운 상행 종점구간을 등록한다")
    @Test
    void createLastUp() {
        StationResponse station = StationAcceptanceTest.createRequest(new StationRequest("신도림"))
                .as(StationResponse.class);

        ExtractableResponse<Response> response = createRequest(line.getId(), new SectionRequest(station.getId(), lastUpStation.getId(), 3));
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());
        ExtractableResponse<Response> selectedResponse = selectRequest(line.getId());
        assertThat(calculateTotalDistance(selectedResponse)).isEqualTo(INIT_DISTANCE + 3);
        List<StationResponse> stations = selectStations(line.getId());
        assertThat(stations).containsExactly(station, lastUpStation,lastDownStation);
    }

    @DisplayName("새로운 하행 종점구간을 등록한다")
    @Test
    void createLastDown() {
        StationResponse station = StationAcceptanceTest.createRequest(new StationRequest("까치산"))
                .as(StationResponse.class);

        ExtractableResponse<Response> response = createRequest(line.getId(), new SectionRequest(lastDownStation.getId(), station.getId(), 3));
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());
        ExtractableResponse<Response> selectedResponse = selectRequest(line.getId());
        assertThat(calculateTotalDistance(selectedResponse)).isEqualTo(INIT_DISTANCE + 3);
        List<StationResponse> stations = selectStations(line.getId());
        assertThat(stations).containsExactly(lastUpStation,lastDownStation,station);
    }

    @DisplayName("역을 삭제한다")
    @Test
    void deleteSection() {
        StationResponse station = StationAcceptanceTest.createRequest(new StationRequest("양천구청"))
                .as(StationResponse.class);
        createRequest(line.getId(), new SectionRequest(station.getId(), lastDownStation.getId(), 3))
                .as(SectionResponse.class);

        ExtractableResponse<Response> deletedResponse = deleteRequest(line.getId(), station.getId());
        assertThat(deletedResponse.statusCode()).isEqualTo(HttpStatus.NO_CONTENT.value());
        ExtractableResponse<Response> selectedResponse = selectRequest(line.getId());
        assertThat(calculateTotalDistance(selectedResponse)).isEqualTo(INIT_DISTANCE);
        List<StationResponse> stations = selectStations(line.getId());
        assertThat(stations).containsExactly(lastUpStation,lastDownStation);
    }

    @DisplayName("노선에 등록되지 않은 역을 삭제한다")
    @Test
    void deleteSectionWithInvalidStation() {
        StationResponse station = StationAcceptanceTest.createRequest(new StationRequest("양천구청"))
                .as(StationResponse.class);

        ExtractableResponse<Response> deletedResponse = deleteRequest(line.getId(), station.getId());
        assertThat(deletedResponse.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    @DisplayName("마지막 하나 남은 구간의 역을 삭제한다")
    @Test
    void deleteLastSection() {
        ExtractableResponse<Response> deletedResponse = deleteRequest(line.getId(), lastUpStation.getId());
        assertThat(deletedResponse.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    private Integer calculateTotalDistance(ExtractableResponse<Response> selectedResponse) {
        List<SectionResponse> sectionResponses = Arrays.asList(selectedResponse.as(SectionResponse[].class));
        return sectionResponses.stream()
                .mapToInt(SectionResponse::getDistance)
                .sum();
    }

    private List<StationResponse> selectStations(Long lineId) {
        LineResponse response = LineAcceptanceTest.selectRequestWithId(lineId)
                .as(LineResponse.class);
        return response.getStations();
    }

    private ExtractableResponse<Response> createRequest(Long lineId, SectionRequest sectionRequest) {
        final String url = "/lines/" + lineId + "/sections";
        return RequestTest.doPost(url, sectionRequest);
    }

    private ExtractableResponse<Response> selectRequest(Long lineId) {
        final String url = "/lines/" + lineId + "/sections";
        return RequestTest.doGet(url);
    }

    private ExtractableResponse<Response> deleteRequest(Long lineId, Long stationId) {
        final String url = "/lines/" + lineId + "/sections?stationId=" + stationId;
        return RequestTest.doDelete(url);
    }
}